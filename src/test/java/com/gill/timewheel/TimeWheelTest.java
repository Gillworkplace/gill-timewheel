package com.gill.timewheel;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import com.gill.timewheel.core.TimeWheelFactory;
import com.gill.timewheel.exception.TimeWheelTerminatedException;

/**
 * TimeWheelTest
 *
 * @author gill
 * @version 2023/11/27
 **/
public class TimeWheelTest {

    /**
     * 过期任务执行
     * 
     * @throws Exception ex
     */
    @Test
    public void testTaskExpired() throws Exception {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        long time = Instant.now().toEpochMilli();
        TimeWheel timeWheel = TimeWheelFactory.create(1000, 60);
        timeWheel.executeAtTime(time - 60 * 1000, "task-expired", () -> future.complete(1));
        Assertions.assertEquals(1, future.get(1000, TimeUnit.MILLISECONDS));
    }

    /**
     * 幂等性任务执行
     * 
     * @throws Exception ex
     */
    @Test
    public void testIdempotence() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger flag = new AtomicInteger(0);
        TimeWheel timeWheel = TimeWheelFactory.create(100, 10);
        long id = 1;
        timeWheel.executeWithDelay(id, 0, "delay-1", TestUtil.wrap(0, flag, latch));
        timeWheel.executeWithDelay(id, 50, "delay-1", TestUtil.wrap(1, flag));
        timeWheel.executeWithDelay(id, 50, "delay-4", TestUtil.wrap(2, flag));
        timeWheel.executeWithDelay(id, 60, "delay-2", TestUtil.wrap(3, flag));
        timeWheel.executeWithDelay(id, 110, "delay-3", TestUtil.wrap(4, flag));
        Assertions.assertTrue(latch.await(3000, TimeUnit.MILLISECONDS));
        Assertions.assertEquals(1, flag.get());
    }

    /**
     * 一个时间轮盘周期内执行延迟任务
     * 
     * @throws Exception ex
     */
    @RepeatedTest(10)
    public void testDelayInPeriod() throws Exception {
        CountDownLatch latch = new CountDownLatch(5);
        AtomicInteger flag = new AtomicInteger(0);
        TimeWheel timeWheel = TimeWheelFactory.create(10, 10);
        timeWheel.executeWithDelay(0, "delay-0", TestUtil.wrap(0, flag, latch));
        timeWheel.executeWithDelay(13, "delay-1", TestUtil.wrap(1, flag, latch));
        timeWheel.executeWithDelay(17, "delay-2", TestUtil.wrap(2, flag, latch));
        timeWheel.executeWithDelay(20, "delay-3", TestUtil.wrap(3, flag, latch));
        timeWheel.executeWithDelay(35, "delay-4", TestUtil.wrap(4, flag, latch));
        Assertions.assertTrue(latch.await(3000, TimeUnit.MILLISECONDS));
        Assertions.assertEquals(0b11111, flag.get());
    }

    /**
     * 多个时间轮盘周期执行延迟任务
     * 
     * @throws Exception ex
     */
    @RepeatedTest(10)
    public void testDelayCrossPeriods() throws Exception {
        CountDownLatch latch = new CountDownLatch(5);
        AtomicInteger flag = new AtomicInteger(0);
        TimeWheel timeWheel = TimeWheelFactory.create(10, 10);
        timeWheel.executeWithDelay(0, "delay-0", TestUtil.wrap(0, flag, latch));
        timeWheel.executeWithDelay(100, "delay-1", TestUtil.wrap(1, flag, latch));
        timeWheel.executeWithDelay(105, "delay-2", TestUtil.wrap(2, flag, latch));
        timeWheel.executeWithDelay(120, "delay-3", TestUtil.wrap(3, flag, latch));
        timeWheel.executeWithDelay(180, "delay-4", TestUtil.wrap(4, flag, latch));
        Assertions.assertTrue(latch.await(3000, TimeUnit.MILLISECONDS));
        Assertions.assertEquals(0b11111, flag.get());
    }

    /**
     * 取消任务执行
     * 
     * @throws Exception ex
     */
    @Test
    public void testCancelTask() throws Exception {
        CountDownLatch latch = new CountDownLatch(3);
        AtomicInteger flag = new AtomicInteger(0);
        TimeWheel timeWheel = TimeWheelFactory.create(10, 10);
        long task1 = timeWheel.executeWithDelay(50, "delay-0", TestUtil.wrap(0, flag, latch));
        long task2 = timeWheel.executeWithDelay(100, "delay-1", TestUtil.wrap(1, flag, latch));
        long task3 = timeWheel.executeWithDelay(120, "delay-2", TestUtil.wrap(2, flag, latch));
        timeWheel.cancel(task1);
        timeWheel.cancel(task2);
        timeWheel.cancel(task3);
        Assertions.assertFalse(latch.await(500, TimeUnit.MILLISECONDS));
        Assertions.assertEquals(0, flag.get());
    }

    /**
     * 指定过期时间，幂等缓存过期
     * 
     * @throws Exception ex
     */
    @Test
    public void testIdempotenceExpired_specific() throws Exception {
        CountDownLatch latch = new CountDownLatch(4);
        AtomicInteger flag = new AtomicInteger(0);
        TimeWheel timeWheel = TimeWheelFactory.create("default", 10, 10, 500);
        timeWheel.executeWithDelay(1, 0, "delay-0", TestUtil.wrap(0, flag, latch));
        timeWheel.executeWithDelay(1, 3, "delay-1", TestUtil.wrap(1, flag, latch));
        timeWheel.executeWithDelay(2, 15, "delay-2", TestUtil.wrap(2, flag, latch));
        Thread.sleep(600);
        timeWheel.executeWithDelay(1, 0, "delay-3", TestUtil.wrap(3, flag, latch));
        timeWheel.executeWithDelay(2, 13, "delay-4", TestUtil.wrap(4, flag, latch));
        Assertions.assertTrue(latch.await(3000, TimeUnit.MILLISECONDS));
        Assertions.assertEquals(0b11101, flag.get());
    }

    /**
     * 当task对象没有被引用后，幂等缓存由GC决定删除
     *
     * @throws Exception ex
     */
    @Test
    public void testIdempotenceExpired_gc() throws Exception {
        AtomicInteger flag = new AtomicInteger(0);
        TimeWheel timeWheel = TimeWheelFactory.create("default", 10, 10, TimeWheelFactory.EXPIRED_BY_GC);
        CompletableFuture<Object> cf = new CompletableFuture<>();
        timeWheel.executeWithDelay(1, 150, "delay-0", () -> {
            flag.accumulateAndGet(1, (x, old) -> x | old);
            cf.complete(1);
        });
        timeWheel.executeWithDelay(1, 50, "delay-1", TestUtil.wrap(1, flag));
        cf.get();

        // gc触发后回收
        System.gc();
        Thread.sleep(500);
        timeWheel.executeWithDelay(1, 0, "delay-2", TestUtil.wrap(2, flag));
        Assertions.assertEquals(0b101, flag.get());
    }

    /**
     * 当延时任务执行完后，幂等缓存就删除
     *
     * @throws Exception ex
     */
    @Test
    public void testIdempotenceExpired_afterExecute() throws Exception {
        AtomicInteger flag = new AtomicInteger(0);
        TimeWheel timeWheel = TimeWheelFactory.create("default", 10, 10, TimeWheelFactory.EXPIRED_AFTER_EXECUTION);
        CompletableFuture<Object> cf = new CompletableFuture<>();
        timeWheel.executeWithDelay(1, 50, "delay-0", () -> {
            flag.accumulateAndGet(1, (x, old) -> x | old);
            cf.complete(1);
        });
        timeWheel.executeWithDelay(1, 10, "delay-1", TestUtil.wrap(1, flag));
        cf.get();
        Thread.sleep(100);
        timeWheel.executeWithDelay(1, 0, "delay-2", TestUtil.wrap(2, flag));
        Assertions.assertEquals(0b101, flag.get());
    }

    /**
     * 关闭timewheel后再调用结果会抛异常
     * 
     * @throws Exception ex
     */
    @Test
    public void testTermination() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger flag = new AtomicInteger(0);
        TimeWheel timeWheel = TimeWheelFactory.create(10, 10);
        timeWheel.executeWithDelay(0, "delay-0", TestUtil.wrap(0, flag, latch));
        Assertions.assertTrue(latch.await(3000, TimeUnit.MILLISECONDS));
        timeWheel.terminate();
        Assertions.assertThrowsExactly(TimeWheelTerminatedException.class,
            () -> timeWheel.executeWithDelay(0, "delay-1", TestUtil.wrap(1, flag)));
        Assertions.assertThrowsExactly(TimeWheelTerminatedException.class, () -> timeWheel.cancel(1));
        Assertions.assertThrowsExactly(TimeWheelTerminatedException.class, () -> timeWheel.delete(1));
        Assertions.assertDoesNotThrow(timeWheel::terminate);
        Assertions.assertEquals(1, flag.get());
    }
}
