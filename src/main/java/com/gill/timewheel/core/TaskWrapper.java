package com.gill.timewheel.core;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

/**
 * TaskWrapper
 *
 * @author gill
 * @version 2023/11/28
 **/
class TaskWrapper extends WeakReference<Task> {

    private final long key;

    public TaskWrapper(Task reference) {
        super(reference);
        this.key = reference.getKey();
    }

    public TaskWrapper(Task reference, ReferenceQueue<? super Task> q) {
        super(reference, q);
        this.key = reference.getKey();
    }

    public long getKey() {
        return key;
    }
}
