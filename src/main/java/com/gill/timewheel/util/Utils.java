package com.gill.timewheel.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import com.gill.timewheel.log.ILogger;
import com.gill.timewheel.log.LoggerFactory;

/**
 * Utils
 *
 * @author gill
 * @version 2023/11/28
 **/
public class Utils {

    private static final ILogger log = LoggerFactory.getLogger(Utils.class);

    /**
     * 等待线程池关闭
     *
     * @param executorService 线程池
     * @param poolName 线程池名
     */
    public static void awaitTermination(ExecutorService executorService, String poolName) {
        long start = System.currentTimeMillis();
        try {
            while (!executorService.awaitTermination(1, TimeUnit.SECONDS)) {
                log.debug("waiting {} termination, duration: {}", poolName, System.currentTimeMillis() - start);
            }
            log.debug("{} is terminated, duration: {}", poolName, System.currentTimeMillis() - start);
        } catch (InterruptedException e) {
            log.warn("{} awaitTermination is interrupted, e: {}", poolName, e);
        }
    }

}
