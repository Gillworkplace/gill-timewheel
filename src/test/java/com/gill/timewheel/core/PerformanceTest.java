package com.gill.timewheel.core;

import com.gill.gutil.statistic.Statistic;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.gill.gutil.statistic.Cost;
import com.gill.gutil.statistic.Counter;
import com.gill.gutil.thread.NamedThreadFactory;
import com.gill.timewheel.TestUtil;
import com.gill.timewheel.TimeWheel;

/**
 * PerformanceTest
 *
 * @author gill
 * @version 2023/11/29
 **/
public class PerformanceTest {

    @Test
    public void testScheduleService() throws Exception {
        ScheduledExecutorService executor =
            new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("schedule-test", true));
        final int delay = 10;
        executor.scheduleAtFixedRate(() -> System.out.println("schedule time: " + Instant.now().toEpochMilli() % 1000),
            0, delay, TimeUnit.MILLISECONDS);
        Thread.sleep(1000);
    }

    @Test
    public void testSleep() throws Exception {
        Statistic sleepError = Statistic.newStatistic("sleepError");
        ExecutorService executor = new ThreadPoolExecutor(4, 4, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
            new NamedThreadFactory("sleep-test-"));
        int QPS = 1000;
        SecureRandom random = SecureRandom.getInstanceStrong();
        int maxSleep = 10;
        CountDownLatch latch = new CountDownLatch(QPS);
        for (int i = 0; i < QPS; i++) {
            executor.execute(() -> {
                long startTime = Instant.now().toEpochMilli();
                int sleep = random.nextInt(maxSleep);
                try {
                    Thread.sleep(sleep);
                } catch (InterruptedException ignored) {
                }
                long realSleepTime = Instant.now().toEpochMilli() - startTime;
                sleepError.merge(realSleepTime - sleep);
                latch.countDown();
            });
        }
        boolean await = latch.await(3000, TimeUnit.MILLISECONDS);
        sleepError.println();
        Assertions.assertTrue(await);
    }

    /**
     * 多线程并发同时调用Execute*方法
     */
    @Test
    public void testAddDelayedTasksWith100ThreadsConcurrently() throws Exception {
        SecureRandom random = SecureRandom.getInstanceStrong();
        int maxDelay = 1000;
        int QPS = 10000;

        CountDownLatch latch = new CountDownLatch(QPS);
        ExecutorService invoker = new ThreadPoolExecutor(4, 4, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
            r -> new Thread(r, "invoker"));
        ExecutorService executor = new ThreadPoolExecutor(4, 4, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
            r -> new Thread(r, "executor"));
        TimeWheel tw = TimeWheelFactory.create("ptest-timewheel", 10, 10, TimeWheelFactory.EXPIRED_BY_GC, executor);
        Counter completeDelayTaskCounter = Counter.newCounter("completeDelayTaskCounter");
        Statistic addTaskCost = Statistic.newStatistic("addTaskCost");
        Statistic delayError = Statistic.newStatistic("delayError");

        Thread.sleep(1000);

        for (int i = 0; i < QPS; i++) {
            invoker.execute(() -> Cost.costMerge(() -> {
                final long startTime = System.nanoTime();
                int delay = random.nextInt(maxDelay);
                tw.executeWithDelay(delay, "test", () -> {
                    long realDelay = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
                    long diff = realDelay - delay;
                    delayError.merge(Math.abs(diff));
                    completeDelayTaskCounter.incr();
                    latch.countDown();
                });
            }, addTaskCost));
        }
        boolean await = latch.await(10000, TimeUnit.MILLISECONDS);
        addTaskCost.println();
        delayError.println();
        Assertions.assertTrue(await);
        Assertions.assertEquals(QPS, completeDelayTaskCounter.get());

        AtomicLong taskCnt = TestUtil.getField(tw, "taskCnt");
        Assertions.assertEquals(0, taskCnt.get());
        Map<Long, Wheel> wheels = TestUtil.getField(tw, "wheels");
        Assertions.assertTrue(wheels.isEmpty() || wheels.size() == 1);
    }
}
