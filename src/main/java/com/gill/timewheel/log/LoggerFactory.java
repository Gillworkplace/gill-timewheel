package com.gill.timewheel.log;

/**
 * LoggerFactory
 *
 * @author gill
 * @version 2023/11/27
 **/
public class LoggerFactory {

    public static <T> ILogger getLogger(Class<T> cls) {
        try {
            return new Slf4jAdapter(org.slf4j.LoggerFactory.getLogger(cls));
        } catch (Exception ignore) {
        }
        return new JdkLogger(cls);
    }
}
