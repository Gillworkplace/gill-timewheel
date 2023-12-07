package com.gill.timewheel.core;

/**
 * Config
 *
 * @author gill
 * @version 2023/12/05
 **/
class Config {

    /**
     * 核心线程数
     */
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
    private long keepAlive = 5L * 60 * 1000;

    /**
     * 阻塞队列大小
     */
    private int blockingQueueSize = Integer.MAX_VALUE;

    /**
     * 任务等待时间
     */
    private long maxWaitTick = -1L;

    public long getMaxWaitTick() {
        return maxWaitTick;
    }

    public void setMaxWaitTick(long maxWaitTick) {
        this.maxWaitTick = maxWaitTick;
    }

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
        this.maxThreadNum = maxThreadNum;
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
