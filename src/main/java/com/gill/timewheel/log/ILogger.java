package com.gill.timewheel.log;

/**
 * ILogger
 *
 * @author gill
 * @version 2023/11/27
 **/
public interface ILogger {

    String getName();

    boolean isTraceEnabled();

    void trace(String format, Object... args);

    boolean isDebugEnabled();

    void debug(String format, Object... args);

    boolean isInfoEnabled();

    void info(String format, Object... args);

    boolean isWarnEnabled();

    void warn(String format, Object... args);

    boolean isErrorEnabled();

    void error(String format, Object... args);
}
