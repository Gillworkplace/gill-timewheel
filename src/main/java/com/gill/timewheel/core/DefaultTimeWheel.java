package com.gill.timewheel.core;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.security.NoSuchAlgorithmException;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.gill.timewheel.TimeWheel;
import com.gill.timewheel.exception.TimeWheelTerminatedException;
import com.gill.timewheel.log.ILogger;
import com.gill.timewheel.log.LoggerFactory;
import com.gill.timewheel.util.NamedThreadFactory;
import com.gill.timewheel.util.Utils;

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

    private static final SecureRandom RANDOM;

    private static final String REMOVE_TASK_PREFIX = "remove-";

    static {
        try {
            RANDOM = SecureRandom.getInstanceStrong();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

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

    private final Config config;

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

    private final Lock lock = new ReentrantLock();

    private final Condition available = lock.newCondition();

    private volatile boolean running = true;

    private final NearlyTask nearlyTask = new NearlyTask();

    private final AtomicInteger taskCnt = new AtomicInteger(0);

    DefaultTimeWheel(String name, long tick, int wheelSize, long expired, Config config,
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
        while (running) {
            long now = getNow();
            cleanAsync();
            lock.lock();
            try {

                // 没有任务时直接长期阻塞
                if (taskCnt.get() == 0) {
                    available.await();
                    System.out.println("start");
                    continue;
                }
                long triggerTime = nearlyTask.getTriggerTime();
                long diff = triggerTime - now;

                // 没有需要执行的任务任务 进入等待
                if (diff > 0) {
                    available.await(diff, TimeUnit.MILLISECONDS);
                    System.out.println("wake up");
                    continue;
                }
                System.out.println("schedule time: " + now % 1000);
                int tIdx = nearlyTask.getTickIdx();
                long wIdx = nearlyTask.getWheelIdx();
                fireTasks(wIdx, tIdx);
                findNextNearlyTask(wIdx, tIdx);
            } catch (InterruptedException e) {
                log.error("timewheel is interrupted, e: {}", e.toString());
                break;
            } finally {
                lock.unlock();
            }
        }
    }

    private void findNextNearlyTask(long wIdx, int tIdx) {
        nearlyTask.reset();
        long maxWaitTick = config.getMaxWaitTick();
        maxWaitTick = maxWaitTick < 0 ? wheelSize : maxWaitTick;
        long wi = wIdx;
        int ti = tIdx;
        for (int i = 1; i <= maxWaitTick; i++) {
            if (++ti == wheelSize) {
                ti = 0;
                wi++;
            }
            Wheel wheel = wheels.get(wi);
            if (wheel != null && wheel.containsTask(ti)) {
                nearlyTask.setNearlyTask(wi, ti, wi * period + ti * tick + stt);
                return;
            }

            // 如果轮盘也没有直接跳过这些tickIdx的处理
            if (wheel == null) {
                i += wheelSize - ti - 1;
                ti = 0;
                wi++;
            }
        }

        // 找不到则设置最小值等待周期
        nearlyTask.setNearlyTask(wi, ti, wi * period + ti * tick + stt);
    }

    private void fireTasks(long wIdx, int tIdx) {
        Wheel wheel = wheels.get(wIdx);
        if (wheel == null) {
            log.warn("wheel {} is disappeared", wIdx);
            return;
        }
        List<Task> tasks = wheel.getAndClearTasks(tIdx);
        if (tasks.isEmpty()) {
            return;
        }
        log.debug("fire wheels[{}][{}]'s tasks", wIdx, tIdx);
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
        assert taskCnt.addAndGet(-cnt) >= 0;
    }

    private void cleanAsync() {
        cleaner.execute(this::removeUselessTask);
    }

    private Runnable taskRunnableWrapper(long key, Runnable runnable) {
        return () -> {
            runnable.run();
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
        long diff = triggerTime - stt;
        long dT = diff / tick * tick;
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
        // delay < tick的时候直接执行
        if (dT > 0 && tT > now) {
            if (addTaskToWheel(task, tT)) {
                return;
            }
        }
        log.debug("timewheel {} execute task {} right now", name, taskName, now);

        // 若延时任务已过期则在当前线程执行
        executeSync(taskName, runnable);
    }

    private boolean addTaskToWheel(Task task, long tT) {
        long wheelIdx = task.getWheelIdx();
        int tickIdx = task.getTickIdx();
        Wheel wheel = wheels.computeIfAbsent(wheelIdx, t -> new Wheel(wheelSize));
        if (wheel.addTask(tickIdx, task)) {
            log.debug("timewheel {} add task {} to the wheels[{}][{}]", name, task.getName(), wheelIdx, tickIdx);
            taskCnt.incrementAndGet();
            lock.lock();
            try {

                // 如果该任务为最近执行的任务则唤醒主线程重新计算睡眠时间
                setNearlyTask(wheelIdx, tickIdx, tT);
            } finally {
                lock.unlock();
            }
            return true;
        }
        return false;
    }

    private void setNearlyTask(long wIdx, int tIdx, long tT) {
        if (nearlyTask.setNearlyTask(wIdx, tIdx, tT)) {
            available.signal();
        }
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
        long diff = expiredTime - stt;
        long dT = diff / tick * tick;
        long wIdx = dT / period;
        int tIdx = (int)(dT % period / tick);
        long tT = dT + stt;
        String taskName = REMOVE_TASK_PREFIX + key;
        Runnable remove = () -> taskCache.remove(key);
        if (dT > 0 && tT > now) {
            Task task = new Task(key, taskName, wIdx, tIdx, defaultTaskExecutor, remove);
            if (addTaskToWheel(task, tT)) {
                return;
            }
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
        Utils.awaitTermination(eventLoop, "timewheel-scheduler");
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
