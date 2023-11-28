package com.gill.timewheel.log;

import org.slf4j.Logger;
import org.slf4j.helpers.NOPLogger;

/**
 * LoggerFactory
 *
 * @author gill
 * @version 2023/11/27
 **/
public class LoggerFactory {

    private static final LogConfig DEFAULT_LOG_CONFIG = new LogConfig();

    public static <T> ILogger getLogger(Class<T> cls) {
        Logger logger = org.slf4j.LoggerFactory.getLogger(cls);
        if (logger instanceof NOPLogger) {
            return new SoutLogger(cls, DEFAULT_LOG_CONFIG);
        }
        return new Slf4jAdapter(logger);
    }
}
