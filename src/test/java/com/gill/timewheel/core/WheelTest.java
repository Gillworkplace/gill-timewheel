package com.gill.timewheel.core;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * WheelTest
 *
 * @author gill
 * @version 2023/12/07
 **/
public class WheelTest {

    @Test
    void testContainsTask_whenIndexIsValid_shouldReturnTrue() {
        // Arrange
        Wheel wheel = new Wheel(2);
        wheel.addTask(0, new Task(1, "test", 1, 1, null, null));

        // Act
        boolean result = wheel.containsTask(0);

        // Assert
        Assertions.assertTrue(result);
    }

    @Test
    void testContainsTask_whenIndexIsInvalid_shouldReturnFalse() {
        // Arrange
        Wheel wheel = new Wheel(2);
        wheel.addTask(0, new Task(1, "test1", 1, 1, null, null));
        wheel.addTask(0, new Task(2, "test2", 1, 1, null, null));

        // Act
        boolean result = wheel.containsTask(2);

        // Assert
        Assertions.assertFalse(result);
    }

    @Test
    void testContainsTask_whenWheelIsEmpty_shouldReturnFalse() {
        // Arrange
        Wheel wheel = new Wheel(2);

        // Act
        boolean result = wheel.containsTask(0);

        // Assert
        Assertions.assertFalse(result);
    }

    @Test
    public void testGetAndClearTasks_WhenIndexIsZero() {
        Wheel wheel = new Wheel(2);
        List<Task> tasks = wheel.getAndClearTasks(0);
        Assertions.assertEquals(Collections.emptyList(), tasks);
    }

    @Test
    public void testGetAndClearTasks_WhenIndexIsPositive() {
        Wheel wheel = new Wheel(2);
        List<Task> tasks = wheel.getAndClearTasks(1);
        Assertions.assertEquals(Collections.emptyList(), tasks);
    }

    @Test
    public void testGetAndClearTasks_WhenIndexIsNegative() {
        Wheel wheel = new Wheel(2);
        List<Task> tasks = wheel.getAndClearTasks(-1);
        Assertions.assertEquals(Collections.emptyList(), tasks);
    }

    @Test
    public void testGetAndClearTasks_WhenTasksAreNotEmpty() {
        Wheel wheel = new Wheel(2);
        wheel.addTask(0, new Task(1, "test1", 1, 1, null, null));
        wheel.addTask(1, new Task(2, "test2", 1, 1, null, null));

        List<Task> tasks = wheel.getAndClearTasks(0);
        Assertions.assertEquals(1, tasks.size());
        Assertions.assertEquals(Collections.emptyList(), wheel.getAndClearTasks(0));
    }
}
