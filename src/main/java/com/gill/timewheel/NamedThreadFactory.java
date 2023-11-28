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

    private final boolean unique;

    private final AtomicInteger threadNumber;

    public NamedThreadFactory(String prefix) {
        this(prefix, false);
    }

    public NamedThreadFactory(String prefix, boolean unique) {
        this.threadNumber = new AtomicInteger(1);
        this.prefix = prefix;
        this.unique = unique;
    }

    public Thread newThread(Runnable r) {
        if (this.unique) {
            return new Thread(r, this.prefix);
        }
        return new Thread(r, this.prefix + this.threadNumber.getAndIncrement());
    }
}