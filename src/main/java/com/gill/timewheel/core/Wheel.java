package com.gill.timewheel.core;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * Wheel
 *
 * @author gill
 * @version 2023/11/27
 **/
class Wheel {

    private final AtomicReferenceArray<List<Task>> wheel;

    public Wheel(int wheelSize) {
        this.wheel = new AtomicReferenceArray<>(wheelSize);
    }

    /**
     * 获取并删除任务
     * 
     * @param idx 索引
     * @return 任务集合
     */
    public List<Task> getAndClearTasks(int idx) {
        return Optional.ofNullable(wheel.getAndUpdate(idx, prev -> Collections.emptyList()))
            .orElse(Collections.emptyList());
    }

    /**
     * 添加任务
     * 
     * @param idx tick索引
     * @param task 任务
     * @return 是否添加成功，如果否说明该tick任务已被处理。
     */
    public boolean addTask(int idx, Task task) {

        // cas 设置数组
        List<Task> tasks = wheel.updateAndGet(idx, prev -> {
            if (prev == null) {
                return new LinkedList<>();
            }
            return prev;
        });

        // 锁数组中的list对象，从而实现分段锁
        // noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (tasks) {
            if (!(tasks instanceof LinkedList)) {
                return false;
            }
            tasks.add(task);
        }
        return true;
    }
}
