package com.gill.timewheel.core;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Task
 *
 * @author gill
 * @version 2023/11/27
 **/
class Task {

    private final long key;

    private final String name;

    private final long wheelIdx;

    private final int tickIdx;

    private final ExecutorService executorService;

    private final Runnable runnable;

    private final AtomicBoolean cancel = new AtomicBoolean(false);

    public Task(long key, String name, long wheelIdx, int tickIdx, ExecutorService executorService, Runnable runnable) {
        this.key = key;
        this.name = name;
        this.wheelIdx = wheelIdx;
        this.tickIdx = tickIdx;
        this.executorService = executorService;
        this.runnable = new RunnableWrapper(name, runnable);
    }

    public long getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    public long getWheelIdx() {
        return wheelIdx;
    }

    public int getTickIdx() {
        return tickIdx;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public Runnable getRunnable() {
        return runnable;
    }

    public boolean isCancel() {
        return cancel.get();
    }

    public boolean cancel() {
        return this.cancel.compareAndSet(false, true);
    }
}
