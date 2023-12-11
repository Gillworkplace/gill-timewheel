package com.gill.timewheel.core;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Config
 *
 * @author gill
 * @version 2023/12/05
 **/
public class TimeWheelConfig {

    /**
     * 核心线程数
     */
    @Min(value = 1, message = "core thread num must be positive")
    private int coreThreadNum;

    /**
     * 最大线程数
     */
    private int maxThreadNum;

    {
        int cpuCores = Runtime.getRuntime().availableProcessors();
        coreThreadNum = 2 * cpuCores;
        maxThreadNum = 2 * cpuCores;
    }

    /**
     * 线程空闲时间
     */
    @PositiveOrZero(message = "blocking queue size must be positive or zero")
    private long keepAlive = 5L * 60 * 1000;

    /**
     * 阻塞队列大小
     */
    @Positive(message = "blocking queue size must be positive")
    @Max(value = Integer.MAX_VALUE, message = "blocking queue size must less than Integer.MAX_VALUE")
    private int blockingQueueSize = Integer.MAX_VALUE;

    public int getCoreThreadNum() {
        return coreThreadNum;
    }

    public void setCoreThreadNum(int coreThreadNum) {
        this.coreThreadNum = coreThreadNum;
    }

    public int getMaxThreadNum() {
        return maxThreadNum;
    }

    public void setMaxThreadNum(int maxThreadNum) {
        this.maxThreadNum = Math.max(maxThreadNum, coreThreadNum);
    }

    public long getKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(long keepAlive) {
        this.keepAlive = keepAlive;
    }

    public int getBlockingQueueSize() {
        return blockingQueueSize;
    }

    public void setBlockingQueueSize(int blockingQueueSize) {
        this.blockingQueueSize = blockingQueueSize;
    }
}
