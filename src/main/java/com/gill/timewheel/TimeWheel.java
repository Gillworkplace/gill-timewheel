package com.gill.timewheel;

import java.util.concurrent.ExecutorService;

/**
 * TimeWheel 时间轮盘使用接口
 *
 * @author gill
 * @version 2023/11/27
 **/
public interface TimeWheel {

    /**
     * 添加延时执行的调度任务
     *
     * @param delay 延迟多久执行单位ms
     * @param taskName 任务名
     * @param runnable 执行方法块
     * @return 任务唯一键
     */
    long executeWithDelay(long delay, String taskName, Runnable runnable);

    /**
     * 添加延时执行的调度任务
     *
     * @param delay 延迟多久执行单位ms
     * @param executor 异步执行的线程池
     * @param taskName 任务名
     * @param runnable 执行方法块
     * @return 任务唯一键
     */
    long executeWithDelay(long delay, ExecutorService executor, String taskName, Runnable runnable);

    /**
     * 添加延时执行的调度任务
     *
     * @param key 唯一键，保证任务幂等执行
     * @param delay 延迟多久执行单位ms
     * @param taskName 任务名
     * @param runnable 执行方法块
     */
    void executeWithDelay(long key, long delay, String taskName, Runnable runnable);

    /**
     * 添加延时执行的调度任务
     *
     * @param key 唯一键，保证任务幂等执行
     * @param delay 延迟多久执行单位ms
     * @param executor 异步执行的线程池
     * @param taskName 任务名
     * @param runnable 执行方法块
     */
    void executeWithDelay(long key, long delay, ExecutorService executor, String taskName, Runnable runnable);

    /**
     * 添加指定时间点执行的调度任务
     *
     * @param executeTime 执行时间戳 ms
     * @param taskName 任务名
     * @param runnable 执行方法块
     * @return 任务唯一键
     */
    long executeAtTime(long executeTime, String taskName, Runnable runnable);

    /**
     * 添加指定时间点执行的调度任务
     *
     * @param executeTime 执行时间戳 ms
     * @param executor 异步执行的线程池
     * @param taskName 任务名
     * @param runnable 执行方法块
     * @return 任务唯一键
     */
    long executeAtTime(long executeTime, ExecutorService executor, String taskName, Runnable runnable);

    /**
     * 添加指定时间点执行的调度任务
     *
     * @param key 唯一键，保证任务幂等执行
     * @param executeTime 执行时间戳 ms
     * @param taskName 任务名
     * @param runnable 执行方法块
     */
    void executeAtTime(long key, long executeTime, String taskName, Runnable runnable);

    /**
     * 添加指定时间点执行的调度任务
     *
     * @param key 唯一键，保证任务幂等执行
     * @param executeTime 执行时间戳 ms
     * @param executor 异步执行的线程池
     * @param taskName 任务名
     * @param runnable 执行方法块
     */
    void executeAtTime(long key, long executeTime, ExecutorService executor, String taskName, Runnable runnable);

    /**
     * 删除执行任务
     * 
     * @param key 唯一键
     */
    void cancel(long key);

    /**
     * 删除执行任务（包括幂等缓存）
     *
     * @param key 唯一键
     */
    void delete(long key);

    /**
     * 终止时间轮盘任务
     */
    void terminate();
}
