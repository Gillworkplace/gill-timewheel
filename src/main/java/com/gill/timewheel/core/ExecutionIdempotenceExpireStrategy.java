package com.gill.timewheel.core;

import java.util.Map;

/**
 * ExecutionIdempotenceExpireStrategy
 *
 * @author gill
 * @version 2023/12/13
 **/
class ExecutionIdempotenceExpireStrategy implements IdempotenceExpireStrategy {

    private final Map<Long, TaskWrapper> taskCache;

    public ExecutionIdempotenceExpireStrategy(Map<Long, TaskWrapper> taskCache) {
        this.taskCache = taskCache;
    }

    @Override
    public void executeTaskPostHandle(long key) {
        taskCache.remove(key);
    }

    @Override
    public boolean isTaskCancel(Task task) {
        boolean cancel = task.isCancel();
        if (cancel) {
            taskCache.remove(task.getKey());
        }
        return cancel;
    }
}
