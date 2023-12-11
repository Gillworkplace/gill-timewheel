package com.gill.timewheel.core;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import java.util.concurrent.ThreadPoolExecutor.DiscardPolicy;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

import com.gill.gutil.log.ILogger;
import com.gill.gutil.log.LoggerFactory;
import com.gill.gutil.statistic.Cost;
import com.gill.gutil.thread.NamedThreadFactory;
import com.gill.gutil.thread.PoolUtil;
import com.gill.timewheel.TimeWheel;
import com.gill.timewheel.exception.TimeWheelTerminatedException;

/**
 * DefaultTimeWheel
 *
 * @author gill
 * @version 2023/11/27
 **/
class DefaultTimeWheel implements TimeWheel, Runnable {

    private static final ILogger log = LoggerFactory.getLogger(DefaultTimeWheel.class);

    private static final RejectedExecutionHandler EVENT_LOOP_POLICY = new AbortPolicy();

    private static final RejectedExecutionHandler CLEANER_POLICY = new DiscardPolicy();

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final String REMOVE_TASK_PREFIX = "remove-key:";

    private final String name;

    /**
     * 轮盘刻度周期
     */
    private final long tick;

    /**
     * 轮盘大小
     */
    private final int wheelSize;

    /**
     * period = tick * wheelSize
     */
    private final long period;

    /**
     * start time 与 tick wheelSize period triggerTime的关系 triggerTime = sts + period * wIdx + tIdx * tick
     */
    private final long stt;

    /**
     * 幂等缓存的过期时间 ms
     */
    private final long expired;

    private final TimeWheelConfig config;

    /**
     * 轮盘
     */
    private final Map<Long, Wheel> wheels = new ConcurrentHashMap<>();

    /**
     * 任务缓存
     */
    private final Map<Long, TaskWrapper> taskCache = new ConcurrentHashMap<>();

    /**
     * 记录被GC回收的taskReference
     */
    private final ReferenceQueue<Object> rq = new ReferenceQueue<>();

    /**
     * tick调度器
     */
    private final ExecutorService eventLoop = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
        new SynchronousQueue<>(), new NamedThreadFactory("timewheel-main", true), EVENT_LOOP_POLICY);

    /**
     * 用于清理幂等任务缓存
     */
    private final ExecutorService cleaner = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
        new SynchronousQueue<>(), new NamedThreadFactory("timewheel-cleaner", true), CLEANER_POLICY);

    private final ExecutorService defaultTaskExecutor;

    private volatile boolean running = true;

    private final AtomicLong taskCnt = new AtomicLong(0);

    DefaultTimeWheel(String name, long tick, int wheelSize, long expired, TimeWheelConfig config,
        ExecutorService defaultTaskExecutor) {
        this.name = name;
        this.tick = tick;
        this.wheelSize = wheelSize;
        this.expired = expired;
        this.config = config;
        this.period = tick * wheelSize;
        this.defaultTaskExecutor = defaultTaskExecutor;
        this.stt = getNow();

        // 启动定时器
        this.eventLoop.execute(this);
    }

    private static long getNow() {
        return (System.nanoTime() + 999999) / 1000000;
    }

    @Override
    public void run() {
        long lastWheelIdx = 0;
        int lastTickIdx = 0;
        while (running) {
            long now = getNow();
            cleanIdempotenceAsync();
            try {

                // 没有任务时进入阻塞状态
                long dT = now - stt;
                long wIdx = dT / period;
                int tIdx = (int)(dT % period / tick);
                log.trace("fire (wheels[{}][{}], wheels[{}][{}]]'tasks", lastWheelIdx, lastTickIdx, wIdx, tIdx);
                final long w = lastWheelIdx;
                final int i = lastTickIdx;
                Cost.cost(() -> fireTasks(w, i, wIdx, tIdx),
                    () -> String.format("fire (wheels[%d][%d], wheels[%d][%d]]'s tasks", w, i, wIdx, tIdx), 5);
                lastWheelIdx = wIdx;
                lastTickIdx = tIdx;
                waitForNextTick(wIdx, tIdx);
            } catch (InterruptedException e) {
                log.error("timewheel is interrupted, e: {}", e.toString());
                break;
            }
        }
    }

    @SuppressWarnings({"LoopStatementThatDoesntLoop"})
    private void waitForNextTick(long wIdx, long tIdx) throws InterruptedException {
        long deadline = wIdx * period + (tIdx + 1) * tick + this.stt;
        for (;;) {
            long waitTime = deadline - getNow();
            if (waitTime > 0) {
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(waitTime));
            }
            return;
        }
    }

    private void fireTasks(long lastWIdx, int lastTIdx, long wIdx, int tIdx) {
        long wi = lastWIdx + (lastTIdx + 1) / wheelSize;
        int ti = (lastTIdx + 1) % wheelSize;
        for (; wi < wIdx || wi == wIdx && ti <= tIdx; wi += (ti + 1) / wheelSize, ti = (ti + 1) % wheelSize) {
            Wheel wheel = wheels.get(wi);
            if (wheel == null) {
                continue;
            }
            List<Task> tasks = wheel.getAndClearTasks(ti);
            if (tasks.isEmpty()) {
                continue;
            }
            log.debug("fire wheels[{}][{}]'s tasks", wi, ti);
            int cnt = 0;
            for (Task task : tasks) {
                ExecutorService executor = task.getExecutorService();
                Runnable run = task.getRunnable();
                if (task.isCancel()) {
                    continue;
                }
                cnt++;
                executor.execute(taskRunnableWrapper(task.getKey(), run));
            }

            // 移除过期的wheel
            removeIfWheelHasBeenExpired(ti, wi);
            taskCnt.addAndGet(-cnt);
        }
    }

    private void removeIfWheelHasBeenExpired(int ti, long wi) {
        if (ti == wheelSize - 1) {
            wheels.remove(wi);
        }
    }

    private void cleanIdempotenceAsync() {
        cleaner.execute(this::removeUselessTask);
    }

    private Runnable taskRunnableWrapper(long key, Runnable runnable) {
        return () -> {
            runnable.run();

            // EXPIRED_AFTER_EXECUTION 才执行
            if (expired == 0) {
                taskCache.remove(key);
            }
        };
    }

    private void removeUselessTask() {
        if (expired > 0) {
            return;
        }
        Reference<?> ref;
        while ((ref = rq.poll()) != null) {
            long key = ((TaskWrapper)ref).getKey();
            log.debug("remove idempotence key: {}", key);
            taskCache.remove(key);
        }
    }

    /**
     * 添加延时执行的调度任务
     *
     * @param delay 延迟多久执行单位ms
     * @param taskName 任务名
     * @param runnable 执行方法块
     * @return 任务唯一键
     */
    @Override
    public long executeWithDelay(long delay, String taskName, Runnable runnable) {
        return executeWithDelay(delay, defaultTaskExecutor, taskName, runnable);
    }

    /**
     * 添加延时执行的调度任务
     *
     * @param delay 延迟多久执行单位ms
     * @param executor 异步执行的线程池
     * @param taskName 任务名
     * @param runnable 执行方法块
     * @return 任务唯一键
     */
    @Override
    public long executeWithDelay(long delay, ExecutorService executor, String taskName, Runnable runnable) {
        long key = RANDOM.nextLong();
        executeWithDelay(key, delay, executor, taskName, runnable);
        return key;
    }

    /**
     * 添加延时执行的调度任务
     *
     * @param key 唯一键，保证任务幂等执行
     * @param delay 延迟多久执行单位ms
     * @param taskName 任务名
     * @param runnable 执行方法块
     */
    @Override
    public void executeWithDelay(long key, long delay, String taskName, Runnable runnable) {
        executeWithDelay(key, delay, defaultTaskExecutor, taskName, runnable);
    }

    /**
     * 添加延时执行的调度任务
     *
     * @param key 唯一键，保证任务幂等执行
     * @param delay 延迟多久执行单位ms
     * @param executor 异步执行的线程池
     * @param taskName 任务名
     * @param runnable 执行方法块
     */
    @Override
    public void executeWithDelay(long key, long delay, ExecutorService executor, String taskName, Runnable runnable) {
        checkState();
        internalExecute(key, delay, executor, taskName, runnable);
    }

    /**
     * 添加指定时间点执行的调度任务
     *
     * @param executeTime 执行时间戳 ms
     * @param taskName 任务名
     * @param runnable 执行方法块
     * @return 任务唯一键
     */
    @Override
    public long executeAtTime(long executeTime, String taskName, Runnable runnable) {
        long key = RANDOM.nextLong();
        executeAtTime(key, executeTime, defaultTaskExecutor, taskName, runnable);
        return key;
    }

    /**
     * 添加指定时间点执行的调度任务
     *
     * @param executeTime 执行时间戳 ms
     * @param executor 异步执行的线程池
     * @param taskName 任务名
     * @param runnable 执行方法块
     * @return 任务唯一键
     */
    @Override
    public long executeAtTime(long executeTime, ExecutorService executor, String taskName, Runnable runnable) {
        long key = RANDOM.nextLong();
        executeAtTime(key, executeTime, executor, taskName, runnable);
        return key;
    }

    /**
     * 添加指定时间点执行的调度任务
     *
     * @param key 唯一键，保证任务幂等执行
     * @param executeTime 执行时间戳 ms
     * @param taskName 任务名
     * @param runnable 执行方法块
     */
    @Override
    public void executeAtTime(long key, long executeTime, String taskName, Runnable runnable) {
        executeAtTime(key, executeTime, defaultTaskExecutor, taskName, runnable);
    }

    /**
     * 添加指定时间点执行的调度任务
     *
     * @param key 唯一键，保证任务幂等执行
     * @param executeTime 执行时间戳 ms
     * @param executor 异步执行的线程池
     * @param taskName 任务名
     * @param runnable 执行方法块
     */
    @Override
    public void executeAtTime(long key, long executeTime, ExecutorService executor, String taskName,
        Runnable runnable) {
        long delay = executeTime - Instant.now().toEpochMilli();
        executeWithDelay(key, delay, executor, taskName, runnable);
    }

    private void internalExecute(long key, long delay, ExecutorService executor, String taskName, Runnable runnable) {
        long now = getNow();
        long triggerTime = now + delay;
        long dT = (triggerTime - stt) / tick * tick;
        long wIdx = dT / period;
        int tIdx = (int)(dT % period / tick);
        long tT = dT + stt;
        Task task = new Task(key, taskName, wIdx, tIdx, executor, runnable);
        TaskWrapper taskWrapper = new TaskWrapper(task, rq);

        // 幂等性校验
        if (taskCache.computeIfAbsent(key, k -> taskWrapper) != taskWrapper) {
            log.debug("Idempotence check, cancel task: {} {}", key, taskName);
            return;
        }

        // 注册过期移除缓存任务
        registerRemoveTask(key);

        // 将任务添加到轮盘中
        // tT在一个tick的周期内执行则直接同步执行
        if (tT > now + tick && addTaskToWheel(task)) {
            return;
        }
        log.debug("timewheel {} execute task {} right now", name, taskName, now);

        // 若延时任务已过期则在当前线程执行
        executeSync(taskName, runnable);
    }

    private boolean addTaskToWheel(Task task) {
        long wheelIdx = task.getWheelIdx();
        int tickIdx = task.getTickIdx();
        Wheel wheel = wheels.computeIfAbsent(wheelIdx, t -> new Wheel(wheelSize));
        if (wheel.addTask(tickIdx, task)) {
            log.debug("timewheel {} add task {} to the wheels[{}][{}]", name, task.getName(), wheelIdx, tickIdx);
            taskCnt.incrementAndGet();
            return true;
        }
        return false;
    }

    private void executeSync(String taskName, Runnable runnable) {
        RunnableWrapper.run(taskName, runnable);
    }

    private void executeAsync(String taskName, Runnable runnable) {
        this.defaultTaskExecutor.execute(new RunnableWrapper(taskName, runnable));
    }

    private void registerRemoveTask(long key) {
        if (expired <= 0) {
            return;
        }
        long now = getNow();
        long expiredTime = expired + now;
        long dT = (expiredTime - stt) / tick * tick;
        long wIdx = dT / period;
        int tIdx = (int)(dT % period / tick);
        long tT = dT + stt;
        String taskName = REMOVE_TASK_PREFIX + key;
        Runnable remove = () -> taskCache.remove(key);
        Task task = new Task(key, taskName, wIdx, tIdx, defaultTaskExecutor, remove);
        if (tT > now + tick && addTaskToWheel(task)) {
            return;
        }
        executeAsync(taskName, remove);
    }

    /**
     * 删除执行任务
     *
     * @param key 唯一键
     */
    @Override
    public void cancel(long key) {
        checkState();
        Task task = Optional.ofNullable(taskCache.get(key)).map(WeakReference::get).orElse(null);
        if (task != null && task.cancel()) {
            log.info("timewheel {} cancels task: {}", name, key);
            taskCnt.decrementAndGet();
        }
    }

    /**
     * 删除执行任务（包括幂等缓存）
     *
     * @param key 唯一键
     */
    @Override
    public void delete(long key) {
        checkState();
        Task task = Optional.ofNullable(taskCache.remove(key)).map(WeakReference::get).orElse(null);
        if (task != null && task.cancel()) {
            log.info("timewheel {} delete task: {}", name, key);
            taskCnt.decrementAndGet();
        }
    }

    /**
     * 终止时间轮盘任务
     */
    @Override
    public void terminate() {
        running = false;
        eventLoop.shutdownNow();
        cleaner.shutdown();
        PoolUtil.awaitTermination(eventLoop, "timewheel-scheduler");
    }

    private void checkState() {
        if (!running) {
            throw new TimeWheelTerminatedException("timewheel " + name + " is terminated");
        }
    }

    /**
     * NearlyTask
     *
     * @author gill
     * @version 2023/12/06
     **/
    static class NearlyTask {

        private long wheelIdx;

        private int tickIdx;

        private long triggerTime;

        public NearlyTask() {
            this.wheelIdx = -1;
            this.tickIdx = -1;
            this.triggerTime = Long.MAX_VALUE;
        }

        public void reset() {
            wheelIdx = -1;
            tickIdx = -1;
            triggerTime = Long.MAX_VALUE;
        }

        public boolean setNearlyTask(long wIdx, int tIdx, long tT) {
            if (triggerTime <= tT) {
                return false;
            }
            wheelIdx = wIdx;
            tickIdx = tIdx;
            triggerTime = tT;
            return true;
        }

        public long getWheelIdx() {
            return wheelIdx;
        }

        public int getTickIdx() {
            return tickIdx;
        }

        public long getTriggerTime() {
            return triggerTime;
        }
    }
}
