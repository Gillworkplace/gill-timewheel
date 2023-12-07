package com.gill.timewheel;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
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
        AtomicInteger flag = new AtomicInteger(0);
        TimeWheel timeWheel = TimeWheelFactory.create(100, 10);
        long id = 1;
        timeWheel.executeWithDelay(id, 0, "delay-1", flag::incrementAndGet);
        Thread.sleep(10);
        timeWheel.executeWithDelay(id, 50, "delay-1", flag::incrementAndGet);
        timeWheel.executeWithDelay(id, 50, "delay-4", flag::incrementAndGet);
        timeWheel.executeWithDelay(id, 60, "delay-2", flag::incrementAndGet);
        timeWheel.executeWithDelay(id, 110, "delay-3", flag::incrementAndGet);
        Thread.sleep(150);
        Assertions.assertEquals(1, flag.get());
    }

    /**
     * 一个时间轮盘周期内执行延迟任务
     * 
     * @throws Exception ex
     */
//    @RepeatedTest(10)
    @Test
    public void testDelayInPeriod() throws Exception {
        AtomicInteger flag = new AtomicInteger(0);
        TimeWheel timeWheel = TimeWheelFactory.create(10, 10);
        timeWheel.executeWithDelay(0, "delay-0", () -> flag.accumulateAndGet(1, (x, old) -> x | old));
        timeWheel.executeWithDelay(13, "delay-1", () -> flag.accumulateAndGet(1 << 1, (x, old) -> x | old));
        timeWheel.executeWithDelay(17, "delay-2", () -> flag.accumulateAndGet(1 << 2, (x, old) -> x | old));
        timeWheel.executeWithDelay(20, "delay-3", () -> flag.accumulateAndGet(1 << 3, (x, old) -> x | old));
        timeWheel.executeWithDelay(35, "delay-4", () -> flag.accumulateAndGet(1 << 4, (x, old) -> x | old));
        Thread.sleep(50);
        Assertions.assertEquals((1 << 5) - 1, flag.get());
    }

    /**
     * 多个时间轮盘周期执行延迟任务
     * 
     * @throws Exception ex
     */
    @RepeatedTest(10)
    public void testDelayCrossPeriods() throws Exception {
        AtomicInteger flag = new AtomicInteger(0);
        TimeWheel timeWheel = TimeWheelFactory.create(10, 10);
        timeWheel.executeWithDelay(0, "delay-0", () -> flag.accumulateAndGet(1, (x, old) -> x | old));
        timeWheel.executeWithDelay(100, "delay-1", () -> flag.accumulateAndGet(1 << 1, (x, old) -> x | old));
        timeWheel.executeWithDelay(105, "delay-2", () -> flag.accumulateAndGet(1 << 2, (x, old) -> x | old));
        timeWheel.executeWithDelay(120, "delay-3", () -> flag.accumulateAndGet(1 << 3, (x, old) -> x | old));
        timeWheel.executeWithDelay(180, "delay-4", () -> flag.accumulateAndGet(1 << 4, (x, old) -> x | old));
        Thread.sleep(250);
        Assertions.assertEquals((1 << 5) - 1, flag.get());
    }

    /**
     * 取消任务执行
     * 
     * @throws Exception ex
     */
    @Test
    public void testCancelTask() throws Exception {
        AtomicInteger flag = new AtomicInteger(0);
        TimeWheel timeWheel = TimeWheelFactory.create(10, 10);
        long task1 =
            timeWheel.executeWithDelay(50, "delay-0", () -> flag.accumulateAndGet(1 << 1, (x, old) -> x | old));
        long task2 =
            timeWheel.executeWithDelay(100, "delay-1", () -> flag.accumulateAndGet(1 << 2, (x, old) -> x | old));
        long task3 =
            timeWheel.executeWithDelay(120, "delay-2", () -> flag.accumulateAndGet(1 << 3, (x, old) -> x | old));
        timeWheel.cancel(task1);
        timeWheel.cancel(task2);
        timeWheel.cancel(task3);
        Thread.sleep(250);
        Assertions.assertEquals(0, flag.get());
    }

    /**
     * 指定过期时间，幂等缓存过期
     * 
     * @throws Exception ex
     */
    @Test
    public void testIdempotenceExpired_specific() throws Exception {
        AtomicInteger flag = new AtomicInteger(0);
        TimeWheel timeWheel = TimeWheelFactory.create("default", 10, 10, 500);
        timeWheel.executeWithDelay(1, 0, "delay-0", () -> flag.accumulateAndGet(1, (x, old) -> x | old));
        timeWheel.executeWithDelay(1, 3, "delay-1", () -> flag.accumulateAndGet(1 << 1, (x, old) -> x | old));
        timeWheel.executeWithDelay(2, 15, "delay-2", () -> flag.accumulateAndGet(1 << 2, (x, old) -> x | old));
        Thread.sleep(600);
        timeWheel.executeWithDelay(1, 0, "delay-3", () -> flag.accumulateAndGet(1 << 3, (x, old) -> x | old));
        timeWheel.executeWithDelay(2, 13, "delay-4", () -> flag.accumulateAndGet(1 << 4, (x, old) -> x | old));
        Thread.sleep(50);
        Assertions.assertEquals(1, flag.get() & 1);
        Assertions.assertEquals(1 << 2, flag.get() & (1 << 2));
        Assertions.assertEquals(1 << 3, flag.get() & (1 << 3));
        Assertions.assertEquals(1 << 4, flag.get() & (1 << 4));
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
        timeWheel.executeWithDelay(1, 50, "delay-1", () -> flag.accumulateAndGet(1 << 1, (x, old) -> x | old));
        cf.get();
        Thread.sleep(500);

        // gc触发后回收
        System.gc();
        Thread.sleep(100);
        timeWheel.executeWithDelay(1, 0, "delay-2", () -> flag.accumulateAndGet(1 << 2, (x, old) -> x | old));
        Assertions.assertEquals(1, flag.get() & 1);
        Assertions.assertEquals(0, flag.get() & (1 << 1));
        Assertions.assertEquals(1 << 2, flag.get() & (1 << 2));
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
        timeWheel.executeWithDelay(1, 10, "delay-1", () -> flag.accumulateAndGet(1 << 1, (x, old) -> x | old));
        cf.get();
        Thread.sleep(100);
        timeWheel.executeWithDelay(1, 0, "delay-2", () -> flag.accumulateAndGet(1 << 2, (x, old) -> x | old));
        Assertions.assertEquals(1, flag.get() & 1);
        Assertions.assertEquals(0, flag.get() & (1 << 1));
        Assertions.assertEquals(1 << 2, flag.get() & (1 << 2));
    }

    /**
     * 关闭timewheel后再调用结果会抛异常
     * 
     * @throws Exception ex
     */
    @Test
    public void testTermination() throws Exception {
        AtomicInteger flag = new AtomicInteger(0);
        TimeWheel timeWheel = TimeWheelFactory.create(10, 10);
        timeWheel.executeWithDelay(0, "delay-0", () -> flag.accumulateAndGet(1, (x, old) -> x | old));
        Thread.sleep(50);
        timeWheel.terminate();
        Assertions.assertThrowsExactly(TimeWheelTerminatedException.class,
            () -> timeWheel.executeWithDelay(0, "delay-1", () -> flag.accumulateAndGet(1 << 1, (x, old) -> x | old)));
        Assertions.assertThrowsExactly(TimeWheelTerminatedException.class, () -> timeWheel.cancel(1));
        Assertions.assertThrowsExactly(TimeWheelTerminatedException.class, () -> timeWheel.delete(1));
        Assertions.assertDoesNotThrow(timeWheel::terminate);
        Assertions.assertEquals(1, flag.get());
    }
}
