package com.gill.timewheel.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.gill.timewheel.core.DefaultTimeWheel.NearlyTask;

/**
 * NearlyTaskTest
 *
 * @author gill
 * @version 2023/12/06
 **/
public class NearlyTaskTest {

    @Test
    public void reset_shouldSetAllFieldsToDefaultValues() {
        NearlyTask task = new NearlyTask();
        task.reset();
        Assertions.assertEquals(-1, task.getWheelIdx());
        Assertions.assertEquals(-1, task.getTickIdx());
        Assertions.assertEquals(Long.MAX_VALUE, task.getTriggerTime());
    }

    @Test
    public void testSetNearlyTask_whenTriggerTimeIsGreaterThanEqualTt() {
        NearlyTask nearlyTask = new NearlyTask();
        long wIdx = 1;
        int tIdx = 2;
        long tT = 10;
        Assertions.assertTrue(nearlyTask.setNearlyTask(wIdx, tIdx, tT));
        Assertions.assertEquals(1, nearlyTask.getWheelIdx());
        Assertions.assertEquals(2, nearlyTask.getTickIdx());
        Assertions.assertEquals(10, nearlyTask.getTriggerTime());
    }

    @Test
    public void testSetNearlyTask_whenTriggerTimeIsLessThanTt() {
        NearlyTask nearlyTask = new NearlyTask();
        long wIdx = 1;
        int tIdx = 2;
        long tT = 5;
        Assertions.assertTrue(nearlyTask.setNearlyTask(wIdx, tIdx, tT));
        Assertions.assertFalse(nearlyTask.setNearlyTask(1, 2, 10));
        Assertions.assertEquals(1, nearlyTask.getWheelIdx());
        Assertions.assertEquals(2, nearlyTask.getTickIdx());
        Assertions.assertEquals(5, nearlyTask.getTriggerTime());
    }
}
