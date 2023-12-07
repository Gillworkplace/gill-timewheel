package com.gill.timewheel.statistic;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CostStatistic
 *
 * @author gill
 * @version 2023/11/23
 **/
public class Cost {

    private static final Logger log = LoggerFactory.getLogger(Cost.class);

    private final String name;

    private double max;

    private double min;

    private double avg;

    private long sampleSize;

    private Queue<Double> rawData = new ConcurrentLinkedQueue<>();

    public Cost(String name, long max, long min, double avg, long sampleSize) {
        this.name = name;
        this.max = max;
        this.min = min;
        this.avg = avg;
        this.sampleSize = sampleSize;
    }

    public static Cost newStatistic(String name) {
        return new Cost(name, Integer.MIN_VALUE, Integer.MAX_VALUE, 0, 0);
    }

    /**
     * 耗时计算
     * 
     * @param func 方法
     * @param funcName 方法名
     * @return 返回结果
     * @param <T> 类型
     */
    public static <T> T cost(Supplier<T> func, String funcName) {
        long start = System.currentTimeMillis();
        T ret = func.get();
        long end = System.currentTimeMillis();
        if (end - start > 0) {
            log.debug("{} cost {}ms", funcName, end - start);
        }
        return ret;
    }

    /**
     * 耗时计算
     *
     * @param func 方法
     * @param statistic 统计数据
     * @return 返回结果
     * @param <T> 类型
     */
    public static <T> T cost(Supplier<T> func, Cost statistic) {
        long start = System.currentTimeMillis();
        T ret = func.get();
        long end = System.currentTimeMillis();
        long sample = end - start;
        statistic.merge(sample);
        return ret;
    }

    /**
     * 耗时计算
     *
     * @param func 方法
     * @param statistic 统计数据
     */
    public static void cost(Runnable func, Cost statistic) {
        long start = System.currentTimeMillis();
        func.run();
        long end = System.currentTimeMillis();
        long sample = end - start;
        statistic.merge(sample);
    }

    public synchronized Cost merge(Cost other) {
        this.max = Math.max(this.max, other.max);
        this.min = Math.min(this.max, other.min);
        this.avg = (this.avg * this.sampleSize + other.avg * other.sampleSize) / (this.sampleSize + other.sampleSize);
        this.sampleSize = this.sampleSize + other.sampleSize;
        return this;
    }

    public synchronized Cost merge(double sample) {
        this.rawData.add(sample);
        this.max = Math.max(this.max, sample);
        this.min = Math.min(this.min, sample);
        this.avg = (this.avg * this.sampleSize + sample) / (this.sampleSize + 1);
        this.sampleSize++;
        return this;
    }

    public double getMax() {
        return max;
    }

    public double getMin() {
        return min;
    }

    public double getAvg() {
        return avg;
    }

    public long getSampleSize() {
        return sampleSize;
    }

    public void println() {
        log.info("{} cost statistic => max: {}, min: {}, avg: {}, sample size: {}", name, this.max, this.min, this.avg,
            this.sampleSize);
    }
}
