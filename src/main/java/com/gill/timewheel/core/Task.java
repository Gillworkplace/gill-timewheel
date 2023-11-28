package com.gill.timewheel.core;

import java.time.Instant;
import java.util.concurrent.ExecutorService;

import com.gill.timewheel.log.ILogger;
import com.gill.timewheel.log.LoggerFactory;

/**
 * Task
 *
 * @author gill
 * @version 2023/11/27
 **/
class Task {

    private static final ILogger log = LoggerFactory.getLogger(Task.class);

    private final long key;

    private final String name;

    private final ExecutorService executorService;

    private final Runnable runnable;

    private final long insertTime;

    private volatile boolean cancel = false;

    public Task(long key, String name, ExecutorService executorService, Runnable runnable, long insertTime) {
        this.key = key;
        this.name = name;
        this.executorService = executorService;
        this.runnable = runnable;
        this.insertTime = insertTime;
    }

    public long getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public Runnable getRunnable() {
        return () -> {
            log.info("[{}] start to execute task {}", Instant.now().toEpochMilli(), getName());
            runnable.run();
            log.info("[{}] finish to execute task {}", Instant.now().toEpochMilli(), getName());
        };
    }

    public long getInsertTime() {
        return insertTime;
    }

    public boolean isCancel() {
        return cancel;
    }

    public void cancel() {
        this.cancel = true;
    }
}
