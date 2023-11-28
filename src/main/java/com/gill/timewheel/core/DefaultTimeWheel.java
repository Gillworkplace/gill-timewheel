package com.gill.timewheel.core;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import java.util.concurrent.TimeUnit;

import com.gill.timewheel.NamedThreadFactory;
import com.gill.timewheel.TimeWheel;
import com.gill.timewheel.exception.TimeWheelTerminatedException;
import com.gill.timewheel.log.ILogger;
import com.gill.timewheel.log.LoggerFactory;
import com.gill.timewheel.util.Utils;

/**
 * DefaultTimeWheel
 *
 * @author gill
 * @version 2023/11/27
 **/
class DefaultTimeWheel implements TimeWheel, Runnable {

    private static final ILogger log = LoggerFactory.getLogger(DefaultTimeWheel.class);

    private static final RejectedExecutionHandler HANDLER = new AbortPolicy();

    private static final SecureRandom RANDOM;

    static {
        try {
            RANDOM = SecureRandom.getInstanceStrong();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private final String name;

    private final long tick;

    private final int wheelSize;

    private final long period;

    /**
     * start timestamp
     */
    private final long sts;

    /**
     * 幂等缓存的过期时间 ms
     */
    private final long expired;

    /**
     * 轮盘
     */
    private final ConcurrentSkipListMap<Long, Wheel> wheels = new ConcurrentSkipListMap<>();

    /**
     * 任务缓存
     */
    private final Map<Long, Task> taskCache = new ConcurrentHashMap<>();

    private final ScheduledExecutorService executors =
        new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("timewheel-main", true), HANDLER);

    private final ExecutorService defaultTaskExecutor;

    /**
     * 时间轮盘任期
     */
    private long term = 0;

    /**
     * 执行的索引位置
     */
    private int tickIdx = 0;

    private volatile boolean running = true;

    DefaultTimeWheel(String name, long tick, int wheelSize, long expired, ExecutorService defaultTaskExecutor) {
        this.name = name;
        this.tick = tick;
        this.wheelSize = wheelSize;
        this.expired = expired;
        this.period = tick * wheelSize;
        this.defaultTaskExecutor = defaultTaskExecutor;
        this.sts = Instant.now().toEpochMilli();

        // 启动定时器
        this.executors.scheduleAtFixedRate(this, 0, tick, TimeUnit.MILLISECONDS);
    }

    @Override
    public void run() {
        try {
            final long t = term;
            final int ti = tickIdx;
            incr();
            Wheel wheel = wheels.computeIfAbsent(t, key -> new Wheel(wheelSize));
            discardsOldWheel();
            List<Task> tasks = wheel.getAndClearTasks(ti);
            if (tasks.isEmpty()) {
                return;
            }
            log.debug("start to execute wheel that term is {} and tickIdx is {}", t, ti);
            for (Task task : tasks) {
                ExecutorService executor = task.getExecutorService();
                Runnable run = task.getRunnable();
                if (task.isCancel()) {
                    continue;
                }
                executor.execute(run);
            }
        } catch (Exception e) {
            log.error("timewheel {} occur exception: {}", name, e.getMessage());
        }
    }

    private void discardsOldWheel() {
        Entry<Long, Wheel> entry;
        while ((entry = wheels.firstEntry()) != null && entry.getKey() < term) {
            long key = entry.getKey();
            wheels.remove(key);
            log.trace("timewheel {} discards wheel-{}", name, key);
        }
    }

    private void incr() {
        tickIdx += 1;
        term += tickIdx / wheelSize;
        tickIdx = tickIdx % wheelSize;
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
        long executeTime = Instant.now().toEpochMilli() + delay;
        executeAtTime(key, executeTime, executor, taskName, runnable);
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
        checkState();
        long now = Instant.now().toEpochMilli();
        long diff = executeTime - sts;
        long term = diff / period;
        int tickIdx = (int)(diff % period / tick);
        Task task = new Task(key, taskName, executor, runnable, now);

        // 幂等性校验
        if (taskCache.computeIfAbsent(key, k -> task) != task) {
            log.debug("Idempotence check, cancel task: {} {}", key, taskName);
            return;
        }

        // 注册过期移除缓存任务
        registerRemoveTask(key);

        // 将任务添加到轮盘中
        if (diff > 0 && executeTime > now) {
            Wheel wheel = wheels.computeIfAbsent(term, t -> new Wheel(wheelSize));
            if (wheel.addTask(tickIdx, task)) {
                log.debug("insert time: {}, timewheel {} add task {} to the wheel that term is {} and tickIdx is {}",
                    now, name, taskName, term, tickIdx);
                return;
            }
        }
        log.debug("timewheel {} execute task {} right now", name, taskName, now);
        executeNow(taskName, executor, runnable);
    }

    private static void executeNow(String taskName, ExecutorService executor, Runnable runnable) {
        executor.execute(() -> {
            log.debug("[{}] start to execute task {}", Instant.now().toEpochMilli(), taskName);
            runnable.run();
            log.debug("[{}] finish to execute task {}", Instant.now().toEpochMilli(), taskName);
        });
    }

    private void registerRemoveTask(long key) {
        if (expired <= 0) {
            return;
        }
        long now = Instant.now().toEpochMilli();
        long expiredTime = expired + now;
        long diff = expiredTime - sts;
        long term = diff / period;
        int tickIdx = (int)(diff % period / tick);
        String taskName = "remove-" + key;
        Runnable remove = () -> taskCache.remove(key);
        if (diff > 0) {
            Wheel wheel = wheels.computeIfAbsent(term, t -> new Wheel(wheelSize));
            Task task = new Task(key, taskName, defaultTaskExecutor, remove, now);
            if (wheel.addTask(tickIdx, task)) {
                return;
            }
        }
        executeNow(taskName, defaultTaskExecutor, remove);
    }

    /**
     * 删除执行任务
     *
     * @param key 唯一键
     */
    @Override
    public void cancel(long key) {
        checkState();
        Task task = taskCache.get(key);
        if (task != null) {
            log.info("timewheel {} cancels task: {}", name, key);
            task.cancel();
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
        Task task = taskCache.remove(key);
        if (task != null) {
            log.info("timewheel {} cancels task: {}", name, key);
            task.cancel();
        }
    }

    /**
     * 终止时间轮盘任务
     */
    @Override
    public void terminate() {
        running = false;
        executors.shutdown();
        Utils.awaitTermination(executors, "timewheel-scheduler");
    }

    private void checkState() {
        if (!running) {
            throw new TimeWheelTerminatedException("timewheel " + name + " is terminated");
        }
    }
}
