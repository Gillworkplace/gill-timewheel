package com.gill.timewheel;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * TimeWheelFactory 时间轮盘工厂
 *
 * @author gill
 * @version 2023/11/27
 **/
public class TimeWheelFactory {

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
        return null;
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
}
