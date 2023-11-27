package com.gill.timewheel;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * NamedThreadFactory
 *
 * @author gill
 * @version 2023/11/27
 **/
public class NamedThreadFactory implements ThreadFactory {
    private final String prefix;

    private final AtomicInteger threadNumber;

    public NamedThreadFactory(String prefix) {
        this.threadNumber = new AtomicInteger(1);
        this.prefix = prefix;
    }

    public Thread newThread(Runnable r) {
        return new Thread(r, this.prefix + this.threadNumber.getAndIncrement());
    }
}