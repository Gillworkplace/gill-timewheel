package com.gill.timewheel.core;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import java.util.concurrent.TimeUnit;

import com.gill.timewheel.NamedThreadFactory;
import com.gill.timewheel.TimeWheel;
import com.gill.timewheel.log.ILogger;
import com.gill.timewheel.log.LoggerFactory;

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

    private static final String DEFAULT_NAME = "default";

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
     * 轮盘
     */
    private final Map<Long, Wheel> wheels = new ConcurrentHashMap<>();

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

    DefaultTimeWheel(long tick, int wheelSize, ExecutorService defaultTaskExecutor) {
        this(DEFAULT_NAME, tick, wheelSize, defaultTaskExecutor);
    }

    DefaultTimeWheel(String name, long tick, int wheelSize, ExecutorService defaultTaskExecutor) {
        this.name = name;
        this.tick = tick;
        this.wheelSize = wheelSize;
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
            List<Task> tasks = wheel.getAndClearTasks(ti);
            if (tasks.isEmpty()) {
                return;
            }
            log.debug("start to execute wheel that term is {} and tickIdx is {}", t, ti);
            for (Task task : tasks) {
                ExecutorService executor = task.getExecutorService();
                Runnable run = task.getRunnable();
                executor.execute(run);
            }
        } catch (Exception e) {
            log.error("timewheel {} occur exception: {}", name, e.getMessage());
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

        if (diff > 0) {
            Wheel wheel = wheels.computeIfAbsent(term, t -> new Wheel(wheelSize));
            if (wheel.addTask(tickIdx, task)) {
                log.debug(
                    "insert time: {}, timewheel {} add task {} to term {} tickIdx {}, the task will execute at {}", now,
                    this.name, taskName, term, tickIdx, executeTime);
                return;
            }
        }
        log.debug("timewheel {} execute task {} at {}", this.name, taskName, now);
        executeNow(taskName, executor, runnable);
    }

    private static void executeNow(String taskName, ExecutorService executor, Runnable runnable) {
        executor.execute(() -> {
            log.debug("[{}] start to execute task {}", Instant.now().toEpochMilli(), taskName);
            runnable.run();
            log.debug("[{}] finish to execute task {}", Instant.now().toEpochMilli(), taskName);
        });
    }

    /**
     * 删除执行任务
     *
     * @param key 唯一键
     */
    @Override
    public void cancel(long key) {

    }
}
