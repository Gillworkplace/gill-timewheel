package com.gill.timewheel;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.gill.timewheel.core.TimeWheelFactory;
import com.gill.timewheel.statistic.Cost;
import com.gill.timewheel.statistic.Counter;
import com.gill.timewheel.util.NamedThreadFactory;

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
        executor.scheduleAtFixedRate(() -> System.out.println("schedule time: " + Instant.now().toEpochMilli() % 1000), 0, delay, TimeUnit.MILLISECONDS);
        Thread.sleep(1000);
    }

    @Test
    public void testSleep() throws Exception {
        Cost sleepError = Cost.newStatistic("sleepError");
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
        int QPS = 1000;

        CountDownLatch latch = new CountDownLatch(QPS);
        ExecutorService executor = new ThreadPoolExecutor(32, 32, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
            new NamedThreadFactory("performance-test-"));
        TimeWheel tw = TimeWheelFactory.create("ptest-timewheel", 10, 100, TimeWheelFactory.EXPIRED_BY_GC, executor);

        // 等待arthas启动
        // Thread.sleep(30000);

        Counter completeDelayTaskCounter = Counter.newCounter("completeDelayTaskCounter");
        Cost addTaskCost = Cost.newStatistic("addTaskCost");
        Cost delayError = Cost.newStatistic("delayError");

        for (int i = 0; i < QPS; i++) {
            executor.execute(() -> Cost.cost(() -> {
                final long startTime = Instant.now().toEpochMilli();
                int delay = random.nextInt(maxDelay);
                tw.executeWithDelay(delay, "test", () -> {
                    long realDelay = Instant.now().toEpochMilli() - startTime;
                    long diff = realDelay - delay;
                    delayError.merge(diff);
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
    }
}
