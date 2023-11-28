package com.gill.timewheel.log;

import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

/**
 * SoutLogger
 *
 * @author gill
 * @version 2023/11/27
 **/
public class JdkLogger implements ILogger {

    private static final String SELF = JdkLogger.class.getName();

    private final String name;

    private final java.util.logging.Logger logger;

    public JdkLogger(Class<?> cls) {
        this.name = cls.getName();
        this.logger = java.util.logging.Logger.getLogger(this.name);
    }

    @Override
    public String getName() {
        return name;
    }

    private void log(String callerFQCN, Level level, String msg, Throwable t) {
        LogRecord record = new LogRecord(level, msg);
        record.setLoggerName(name);
        record.setThrown(t);
        fillCallerData(callerFQCN, record);
        logger.log(record);
    }

    private static void fillCallerData(String callerFQCN, LogRecord record) {
        StackTraceElement[] steArray = new Throwable().getStackTrace();

        int selfIndex = -1;
        for (int i = 0; i < steArray.length; i++) {
            final String className = steArray[i].getClassName();
            if (className.equals(callerFQCN)) {
                selfIndex = i;
                break;
            }
        }

        int found = -1;
        for (int i = selfIndex + 1; i < steArray.length; i++) {
            final String className = steArray[i].getClassName();
            if (!(className.equals(callerFQCN))) {
                found = i;
                break;
            }
        }

        if (found != -1) {
            StackTraceElement ste = steArray[found];
            record.setSourceClassName(ste.getClassName());
            record.setSourceMethodName(ste.getMethodName());
        }
    }

    @Override
    public boolean isTraceEnabled() {
        return this.logger.isLoggable(Level.FINEST);
    }

    @Override
    public void trace(String format, Object... args) {
        if (logger.isLoggable(Level.FINEST)) {
            FormattingTuple ft = MessageFormatter.arrayFormat(format, args);
            log(SELF, Level.FINEST, ft.getMessage(), ft.getThrowable());
        }
    }

    @Override
    public boolean isDebugEnabled() {
        return this.logger.isLoggable(Level.FINE);
    }

    @Override
    public void debug(String format, Object... args) {
        if (logger.isLoggable(Level.FINE)) {
            FormattingTuple ft = MessageFormatter.arrayFormat(format, args);
            log(SELF, Level.FINE, ft.getMessage(), ft.getThrowable());
        }
    }

    @Override
    public boolean isInfoEnabled() {
        return this.logger.isLoggable(Level.INFO);
    }

    @Override
    public void info(String format, Object... args) {
        if (logger.isLoggable(Level.INFO)) {
            FormattingTuple ft = MessageFormatter.arrayFormat(format, args);
            log(SELF, Level.INFO, ft.getMessage(), ft.getThrowable());
        }
    }

    @Override
    public boolean isWarnEnabled() {
        return this.logger.isLoggable(Level.WARNING);
    }

    @Override
    public void warn(String format, Object... args) {
        if (logger.isLoggable(Level.WARNING)) {
            FormattingTuple ft = MessageFormatter.arrayFormat(format, args);
            log(SELF, Level.WARNING, ft.getMessage(), ft.getThrowable());
        }
    }

    @Override
    public boolean isErrorEnabled() {
        return this.logger.isLoggable(Level.SEVERE);
    }

    @Override
    public void error(String format, Object... args) {
        if (logger.isLoggable(Level.SEVERE)) {
            FormattingTuple ft = MessageFormatter.arrayFormat(format, args);
            log(SELF, Level.SEVERE, ft.getMessage(), ft.getThrowable());
        }
    }

}
