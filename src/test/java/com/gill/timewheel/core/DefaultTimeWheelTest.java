package com.gill.timewheel.core;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.gill.gutil.thread.NamedThreadFactory;
import com.gill.timewheel.TestUtil;
import com.gill.timewheel.TimeWheel;

/**
 * DefaultTimeWheelTest
 *
 * @author gill
 * @version 2023/12/07
 **/
public class DefaultTimeWheelTest {

    @Test
    public void testFireTasks_NoTasksInWheel() {
        TimeWheel timeWheel = TimeWheelFactory.create(10, 10);
        TestUtil.invoke(timeWheel, "fireTasks", new Class[] {long.class, int.class, long.class, int.class},
            new Object[] {0, 0, 0, 0});
    }

    @Test
    public void testFireTasks_SomeMatchingTasks() throws InterruptedException {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(3, 3, 0, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(), new NamedThreadFactory("test-"));
        TimeWheel timeWheel = TimeWheelFactory.create(10, 10);
        Map<Long, Wheel> wheels = TestUtil.getField(timeWheel, "wheels");
        AtomicLong taskCnt = TestUtil.getField(timeWheel, "taskCnt");
        Wheel wheel = wheels.computeIfAbsent(0L, key -> new Wheel(10));
        AtomicInteger flag = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(3);
        wheel.addTask(0, new Task(0, "task1", 0, 0, executor, TestUtil.wrap(0, flag)));
        wheel.addTask(1, new Task(0, "task2", 0, 0, executor, TestUtil.wrap(1, flag, latch)));
        wheel.addTask(1, new Task(0, "task3", 0, 0, executor, TestUtil.wrap(2, flag, latch)));
        wheel.addTask(2, new Task(0, "task4", 0, 0, executor, TestUtil.wrap(3, flag, latch)));
        taskCnt.addAndGet(4);
        TestUtil.invoke(timeWheel, "fireTasks", new Class[] {long.class, int.class, long.class, int.class},
            new Object[] {0, 0, 0, 2});
        Assertions.assertTrue(latch.await(500, TimeUnit.MILLISECONDS));
        Assertions.assertEquals(0b1110, flag.get());
    }

    @Test
    public void testFireTasks_SomeCrossWheelTasks() throws InterruptedException {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(3, 3, 0, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(), new NamedThreadFactory("test-"));
        TimeWheel timeWheel = TimeWheelFactory.create(10, 10);
        Map<Long, Wheel> wheels = TestUtil.getField(timeWheel, "wheels");
        AtomicLong taskCnt = TestUtil.getField(timeWheel, "taskCnt");
        Wheel wheel0 = wheels.computeIfAbsent(0L, key -> new Wheel(10));
        Wheel wheel1 = wheels.computeIfAbsent(1L, key -> new Wheel(10));
        AtomicInteger flag = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(4);
        wheel0.addTask(8, new Task(0, "task1", 0, 0, executor, TestUtil.wrap(0, flag, latch)));
        wheel0.addTask(9, new Task(0, "task2", 0, 0, executor, TestUtil.wrap(1, flag, latch)));
        wheel1.addTask(0, new Task(0, "task3", 0, 0, executor, TestUtil.wrap(2, flag, latch)));
        wheel1.addTask(1, new Task(0, "task4", 0, 0, executor, TestUtil.wrap(3, flag, latch)));
        taskCnt.addAndGet(4);
        TestUtil.invoke(timeWheel, "fireTasks", new Class[] {long.class, int.class, long.class, int.class},
            new Object[] {0, 7, 1, 1});
        Assertions.assertTrue(latch.await(500, TimeUnit.MILLISECONDS));
        Assertions.assertEquals(0b1111, flag.get());
    }
}
