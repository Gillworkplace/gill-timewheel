package com.gill.timewheel.core;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.DiscardPolicy;
import java.util.concurrent.TimeUnit;

import com.gill.gutil.log.ILogger;
import com.gill.gutil.log.LoggerFactory;
import com.gill.gutil.thread.NamedThreadFactory;

/**
 * GcIdempotenceExpireStrategy
 *
 * @author gill
 * @version 2023/12/13
 **/
class GcIdempotenceExpireStrategy implements IdempotenceExpireStrategy {

    private static final ILogger log = LoggerFactory.getLogger(GcIdempotenceExpireStrategy.class);

    private static final RejectedExecutionHandler CLEANER_POLICY = new DiscardPolicy();

    /**
     * 用于清理幂等任务缓存
     */
    private final ExecutorService cleaner = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
        new SynchronousQueue<>(), new NamedThreadFactory("timewheel-cleaner", true), CLEANER_POLICY);

    private final ReferenceQueue<Object> rq = new ReferenceQueue<>();

    private final Map<Long, TaskWrapper> taskCache;

    public GcIdempotenceExpireStrategy(Map<Long, TaskWrapper> taskCache) {
        this.taskCache = taskCache;
    }

    @Override
    public TaskWrapper newTaskWrapper(Task task) {
        return new TaskWrapper(task, this.rq);
    }

    @Override
    public void loopPreHandle() {
        cleaner.execute(() -> {
            Reference<?> ref;
            while ((ref = rq.poll()) != null) {
                long key = ((TaskWrapper)ref).getKey();
                log.debug("remove idempotence key: {}", key);
                taskCache.remove(key);
            }
        });
    }
}
