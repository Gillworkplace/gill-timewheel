
package com.gill.timewheel.core;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TimeWheelFactoryTest {

    private final ExecutorService defaultExecutor = Executors.newSingleThreadExecutor();

    @Test
    void testCreate1() {
        assertNotNull(TimeWheelFactory.create(10, 10));
    }

    @Test
    void testCreate2() {
        assertNotNull(TimeWheelFactory.create(10, 10, defaultExecutor));
    }

    @Test
    void testCreate3() {
        assertNotNull(TimeWheelFactory.create("name", 10, 10, 0L));
    }

    @Test
    void testCreate4() {
        assertNotNull(TimeWheelFactory.create("name", 10, 10, 0L, defaultExecutor));
    }

    @Test
    public void testCreateWithNullName() {
        assertThrowsExactly(NullPointerException.class,
            () -> TimeWheelFactory.create(null, 1000L, 100, 60000L, defaultExecutor, new TimeWheelConfig()));

    }

    @Test
    public void testCreateWithInvalidTick() {
        assertThrowsExactly(IllegalArgumentException.class,
            () -> TimeWheelFactory.create("test", -1L, 100, 60000L, defaultExecutor, new TimeWheelConfig()));
    }

    @Test
    public void testCreateWithInvalidWheelSize() {
        assertThrowsExactly(IllegalArgumentException.class,
            () -> TimeWheelFactory.create("test", 1000L, -1, 60000L, defaultExecutor, new TimeWheelConfig()));
    }

    @Test
    public void testCreateWithInvalidExpired() {
        assertThrowsExactly(IllegalArgumentException.class,
            () -> TimeWheelFactory.create("test", 1000L, 100, -2L, defaultExecutor, new TimeWheelConfig()));
    }

    @Test
    public void testCreateWithNullExecutor() {
        assertThrowsExactly(NullPointerException.class,
            () -> TimeWheelFactory.create("test", 1000L, 100, 60000L, null, new TimeWheelConfig()));
    }

    @Test
    public void testCreateWithInvalidConfig() {
        TimeWheelConfig invalidConfig = new TimeWheelConfig();
        invalidConfig.setCoreThreadNum(-1);
        invalidConfig.setMaxThreadNum(-1);
        invalidConfig.setBlockingQueueSize(0);
        invalidConfig.setKeepAlive(-1);
        assertThrowsExactly(IllegalArgumentException.class,
            () -> TimeWheelFactory.create("test", 1000L, 100, 60000L, defaultExecutor, invalidConfig));
    }

    @Test
    public void testCreateValidConfig() {
        TimeWheelConfig config = new TimeWheelConfig();
        Assertions.assertNotNull(TimeWheelFactory.create("test", 1000L, 100, 60000L, defaultExecutor, config));
    }
}
