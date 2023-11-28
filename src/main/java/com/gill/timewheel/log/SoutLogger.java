package com.gill.timewheel.log;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

/**
 * SoutLogger
 *
 * @author gill
 * @version 2023/11/27
 **/
public class SoutLogger implements ILogger {

    private final String name;

    private final LogConfig logConfig;

    public SoutLogger(Class<?> cls, LogConfig logConfig) {
        this.name = cls.getName();
        this.logConfig = logConfig;
    }

    @Override
    public String getName() {
        return name;
    }

    private void log(LogLevel level, String msg) {
        Thread thread = Thread.currentThread();
        StackTraceElement[] stackTrace = thread.getStackTrace();
        String line = "";
        for (StackTraceElement ste : stackTrace) {
            if(ste.getClassName().equals(name)) {
                line = "(" + ste.getFileName() + ":" + ste.getLineNumber() + ")";
                break;
            }
        }
        System.out.println(Instant.now().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            + " [" + thread.getName() + "] " + level.name() + " " + name + line + " - " + msg);
    }

    @Override
    public boolean isTraceEnabled() {
        return LogLevel.TRACE.getValue() >= logConfig.getLogLevel().getValue();
    }

    @Override
    public void trace(String format, Object... args) {
        if (isTraceEnabled()) {
            FormattingTuple ft = MessageFormatter.arrayFormat(format, args);
            log(LogLevel.TRACE, ft.getMessage());
        }
    }

    @Override
    public boolean isDebugEnabled() {
        return LogLevel.DEBUG.getValue() >= logConfig.getLogLevel().getValue();
    }

    @Override
    public void debug(String format, Object... args) {
        if (isDebugEnabled()) {
            FormattingTuple ft = MessageFormatter.arrayFormat(format, args);
            log(LogLevel.DEBUG, ft.getMessage());
        }
    }

    @Override
    public boolean isInfoEnabled() {
        return LogLevel.INFO.getValue() >= logConfig.getLogLevel().getValue();
    }

    @Override
    public void info(String format, Object... args) {
        if (isInfoEnabled()) {
            FormattingTuple ft = MessageFormatter.arrayFormat(format, args);
            log(LogLevel.INFO, ft.getMessage());
        }
    }

    @Override
    public boolean isWarnEnabled() {
        return LogLevel.WARN.getValue() >= logConfig.getLogLevel().getValue();
    }

    @Override
    public void warn(String format, Object... args) {
        if (isWarnEnabled()) {
            FormattingTuple ft = MessageFormatter.arrayFormat(format, args);
            log(LogLevel.WARN, ft.getMessage());
        }
    }

    @Override
    public boolean isErrorEnabled() {
        return LogLevel.ERROR.getValue() >= logConfig.getLogLevel().getValue();
    }

    @Override
    public void error(String format, Object... args) {
        if (isErrorEnabled()) {
            FormattingTuple ft = MessageFormatter.arrayFormat(format, args);
            log(LogLevel.ERROR, ft.getMessage());
        }
    }

}
