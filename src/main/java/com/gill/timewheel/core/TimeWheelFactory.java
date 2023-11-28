package com.gill.timewheel.core;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.gill.timewheel.NamedThreadFactory;
import com.gill.timewheel.TimeWheel;

/**
 * TimeWheelFactory 时间轮盘工厂
 *
 * @author gill
 * @version 2023/11/27
 **/
public class TimeWheelFactory {

    private static final String DEFAULT_NAME = "default";

    static class DefaultTimeWheelExecutor {
        private static final ThreadPoolExecutor INSTANCE;

        static {
            int cpuCores = Runtime.getRuntime().availableProcessors();
            INSTANCE = new ThreadPoolExecutor(cpuCores + 1, 2 * cpuCores, 5L * 60 * 1000, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(10), new NamedThreadFactory("timewheel-"));
        }
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
        return new DefaultTimeWheel(DEFAULT_NAME, tick, wheelSize, 5L * 60 * 1000, defaultExecutor);
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
     * @param name 轮盘名称
     * @param tick tick
     * @param wheelSize 轮盘大小
     * @param expired 幂等任务过期时间
     * @return 轮盘
     */
    public static TimeWheel create(String name, long tick, int wheelSize, long expired) {
        return new DefaultTimeWheel(name, tick, wheelSize, expired, DefaultTimeWheelExecutor.INSTANCE);
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
        return new DefaultTimeWheel(name, tick, wheelSize, expired, defaultExecutor);
    }
}
