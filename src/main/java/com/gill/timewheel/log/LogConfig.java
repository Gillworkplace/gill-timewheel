package com.gill.timewheel.log;

/**
 * LogConfig
 *
 * @author gill
 * @version 2023/11/28
 **/
public class LogConfig {

    private LogLevel logLevel = LogLevel.TRACE;

    public LogLevel getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(LogLevel logLevel) {
        this.logLevel = logLevel;
    }
}
