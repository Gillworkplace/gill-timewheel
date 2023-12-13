package com.gill.timewheel.core;

import java.util.function.BiConsumer;

/**
 * IdempotenceExpireStrategy
 *
 * @author gill
 * @version 2023/12/13
 **/
interface IdempotenceExpireStrategy {

    /**
     * 创建一个新的TaskWrapper对象，并将给定的Task对象作为参数传入构造函数中。
     *
     * @param task 传入的Task对象
     * @return 创建的TaskWrapper对象
     */
    default TaskWrapper newTaskWrapper(Task task) {
        return new TaskWrapper(task);
    }

    /**
     * 默认的幂等性检查通过后处理方法，用于将任务添加到轮转队列并异步执行
     *
     * @param key id
     * @param handle 处理器
     */
    default void idempotentCheckPassesPostHandle(long key, BiConsumer<Long, String> handle) {}

    /**
     * 默认的循环前处理方法。
     */
    default void loopPreHandle() {}

    /**
     * 判断任务是否取消
     *
     * @param task 任务对象
     * @return 如果任务已取消则返回true，否则返回false
     */
    default boolean isTaskCancel(Task task) {
        return task.isCancel();
    }

    /**
     * 执行任务的后处理
     *
     * @param key 长整数键值
     */
    default void executeTaskPostHandle(long key) {}
}
