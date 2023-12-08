package com.gill.timewheel.core;

import com.gill.gutil.thread.NamedThreadFactory;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.gill.timewheel.TimeWheel;

/**
 * TimeWheelFactory 时间轮盘工厂
 *
 * @author gill
 * @version 2023/11/27
 **/
public class TimeWheelFactory {

    private static final String DEFAULT_NAME = "default";

    public static final long EXPIRED_BY_GC = -1;

    public static final long EXPIRED_AFTER_EXECUTION = 0;

    private static final Config DEFAULT_CONFIG = new Config();

    static class DefaultTimeWheelExecutor {
        private static final ThreadPoolExecutor INSTANCE = new ThreadPoolExecutor(DEFAULT_CONFIG.getCoreThreadNum(),
            DEFAULT_CONFIG.getMaxThreadNum(), DEFAULT_CONFIG.getKeepAlive(), TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(DEFAULT_CONFIG.getBlockingQueueSize()), new NamedThreadFactory("timewheel-"));
    }

    /**
     * 创建时间轮盘
     *
     * @param tick tick
     * @param wheelSize wheelSize
     * @return 时间轮盘
     */
    public static TimeWheel create(long tick, int wheelSize) {
        return create(tick, wheelSize, DefaultTimeWheelExecutor.INSTANCE);
    }

    /**
     * 创建时间轮盘
     *
     * @param tick tick
     * @param wheelSize wheelSize
     * @param defaultExecutor 默认异步执行的线程池
     * @return 时间轮盘
     */
    public static TimeWheel create(long tick, int wheelSize, ExecutorService defaultExecutor) {
        return new DefaultTimeWheel(DEFAULT_NAME, tick, wheelSize, -1, DEFAULT_CONFIG, defaultExecutor);
    }

    /**
     * 创建时间轮盘
     *
     * @param name 轮盘名称
     * @param tick tick
     * @param wheelSize 轮盘大小
     * @param expired 幂等任务过期时间
     * @return 轮盘
     */
    public static TimeWheel create(String name, long tick, int wheelSize, long expired) {
        return new DefaultTimeWheel(name, tick, wheelSize, expired, DEFAULT_CONFIG, DefaultTimeWheelExecutor.INSTANCE);
    }

    /**
     * 创建时间轮盘
     *
     * @param name 轮盘名称
     * @param tick tick
     * @param wheelSize 轮盘大小
     * @param expired 幂等任务过期时间
     * @param defaultExecutor 默认执行器
     * @return 轮盘
     */
    public static TimeWheel create(String name, long tick, int wheelSize, long expired,
        ExecutorService defaultExecutor) {
        return new DefaultTimeWheel(name, tick, wheelSize, expired, DEFAULT_CONFIG, defaultExecutor);
    }
}
