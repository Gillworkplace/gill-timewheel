package com.gill.timewheel.core;

import java.time.Instant;

import com.gill.timewheel.log.ILogger;
import com.gill.timewheel.log.LoggerFactory;

/**
 * RunnableWrapper
 *
 * @author gill
 * @version 2023/11/30
 **/
public class RunnableWrapper implements Runnable {

    private static final ILogger log = LoggerFactory.getLogger(RunnableWrapper.class);

    private final String taskName;

    private final Runnable runnable;

    public RunnableWrapper(String taskName, Runnable runnable) {
        this.taskName = taskName;
        this.runnable = runnable;
    }

    @Override
    public void run() {
        run(taskName, runnable);
    }

    public static void run(String taskName, Runnable runnable) {
        log.info("[{}] start to execute task {}", Instant.now().toEpochMilli(), taskName);
        runnable.run();
        log.info("[{}] finish to execute task {}", Instant.now().toEpochMilli(), taskName);
    }
}
