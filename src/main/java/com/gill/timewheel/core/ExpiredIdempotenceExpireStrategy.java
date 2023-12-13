package com.gill.timewheel.core;

import java.util.function.BiConsumer;

/**
 * ExpiredIdempotenceExpireStrategy
 *
 * @author gill
 * @version 2023/12/13
 **/
class ExpiredIdempotenceExpireStrategy implements IdempotenceExpireStrategy {

    private static final String REMOVE_TASK_PREFIX = "remove-key:";

    @Override
    public void idempotentCheckPassesPostHandle(long key, BiConsumer<Long, String> handle) {
        handle.accept(key, REMOVE_TASK_PREFIX);
    }
}
