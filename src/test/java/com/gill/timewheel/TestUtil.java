package com.gill.timewheel;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * TestUtil
 *
 * @author gill
 * @version 2023/12/07
 **/
public class TestUtil {

    public static <T> T getField(Object target, String fieldName) {
        try {
            List<Field> fields = new ArrayList<>();
            Class<?> clazz = target.getClass();
            while (clazz != Object.class) {
                fields.addAll(Arrays.stream(clazz.getDeclaredFields()).collect(Collectors.toList()));
                clazz = clazz.getSuperclass();
            }
            for (Field field : fields) {
                if (fieldName.equals(field.getName())) {
                    field.setAccessible(true);
                    return (T)field.get(target);
                }
            }
            throw new NoSuchFieldException();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T invoke(Object target, String methodName, Class<?>[] clazz, Object[] args) {
        try {
            Method method = target.getClass().getDeclaredMethod(methodName, clazz);
            method.setAccessible(true);
            return (T)method.invoke(target, args);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Runnable wrap(int bit, AtomicInteger flag, CountDownLatch latch) {
        return () -> {
            flag.accumulateAndGet(1 << bit, (prev, x) -> prev | x);
            if (latch != null) {
                latch.countDown();
            }
        };
    }

    public static Runnable wrap(int bit, AtomicInteger flag) {
        return wrap(bit, flag, null);
    }
}
