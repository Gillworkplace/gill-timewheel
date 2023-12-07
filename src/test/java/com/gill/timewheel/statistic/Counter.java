package com.gill.timewheel.statistic;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import sun.misc.Cleaner;

/**
 * Counter counter统一管理
 *
 * @author gill
 * @version 2023/11/29
 **/
public class Counter {

    private static final Map<String, WeakReference<Counter>> COUNTERS = new ConcurrentHashMap<>();

    private final String name;

    private final AtomicLong cnt;

    private final Cleaner cleaner;

    private static class RemoveCounter implements Runnable {

        private final String key;

        private RemoveCounter(String key) {
            this.key = key;
        }

        @Override
        public void run() {
            Counter.COUNTERS.remove(key);
        }
    }

    private Counter(String name, long cnt) {
        this.name = name;
        this.cnt = new AtomicLong(cnt);
        this.cleaner = Cleaner.create(this, new RemoveCounter(name));
    }

    public static Counter newCounter(String name) {
        return newCounter(name, 0L);
    }

    public static Counter newCounter(String name, long initValue) {
        Counter counter = new Counter(name, initValue);
        COUNTERS.put(name, new WeakReference<>(counter));
        return counter;
    }

    public String getName() {
        return name;
    }

    public long get() {
        return cnt.get();
    }

    /**
     * +1
     */
    public void incr() {
        cnt.incrementAndGet();
    }

    /**
     * +val
     * 
     * @param val val
     */
    public void incr(long val) {
        cnt.accumulateAndGet(val, Long::sum);
    }

    /**
     * -1
     */
    public void decr() {
        cnt.decrementAndGet();
    }

    /**
     * -val
     * 
     * @param val val
     */
    public void decr(long val) {
        incr(-val);
    }

    public void set(long val) {
        cnt.set(val);
    }

    /**
     * 返回所有counter的值
     * 
     * @return counter
     */
    public static Map<String, Long> getAll() {
        Map<String, Long> all = new HashMap<>(COUNTERS.size());
        for (Entry<String, WeakReference<Counter>> entry : COUNTERS.entrySet()) {
            Optional.ofNullable(entry.getValue()).map(WeakReference::get).map(Counter::get).ifPresent(cnt -> {
                all.put(entry.getKey(), cnt);
            });
        }
        return all;
    }
}
