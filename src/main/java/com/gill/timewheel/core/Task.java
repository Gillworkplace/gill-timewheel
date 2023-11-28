package com.gill.timewheel.core;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final long key;

    private final String name;

    private final ExecutorService executorService;

    private final Runnable runnable;

    private final long insertTime;

    private final long delay;

    private volatile boolean cancel = false;

    public Task(long key, String name, ExecutorService executorService, Runnable runnable, long insertTime,
        long delay) {
        this.key = key;
        this.name = name;
        this.executorService = executorService;
        this.runnable = runnable;
        this.insertTime = insertTime;
        this.delay = delay;
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
            log.debug("[{}] start to execute task-{}", Instant.now().atZone(ZoneId.systemDefault()).format(FORMATTER),
                getName());
            runnable.run();
            log.debug("[{}] finish to execute task-{}", Instant.now().atZone(ZoneId.systemDefault()).format(FORMATTER),
                getName());
        };
    }

    public long getInsertTime() {
        return insertTime;
    }

    public long getDelay() {
        return delay;
    }

    public boolean isCancel() {
        return cancel;
    }

    public void cancel() {
        this.cancel = true;
    }
}
