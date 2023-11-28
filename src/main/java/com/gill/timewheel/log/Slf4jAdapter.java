package com.gill.timewheel.log;

import org.slf4j.Logger;

/**
 * Slf4jAdapter
 *
 * @author gill
 * @version 2023/11/27
 **/
public class Slf4jAdapter implements ILogger {

    private final Logger logger;

    public Slf4jAdapter(Logger logger) {
        this.logger = logger;
    }

    @Override
    public String getName() {
        return this.logger.getName();
    }

    @Override
    public boolean isTraceEnabled() {
        return this.logger.isTraceEnabled();
    }

    @Override
    public void trace(String format, Object... args) {
        if (isTraceEnabled()) {
            this.logger.trace(format, args);
        }
    }

    @Override
    public boolean isDebugEnabled() {
        return this.logger.isDebugEnabled();
    }

    @Override
    public void debug(String format, Object... args) {
        if (isDebugEnabled()) {
            this.logger.debug(format, args);
        }
    }

    @Override
    public boolean isInfoEnabled() {
        return this.logger.isInfoEnabled();
    }

    @Override
    public void info(String format, Object... args) {
        if (isInfoEnabled()) {
            this.logger.info(format, args);
        }
    }

    @Override
    public boolean isWarnEnabled() {
        return this.logger.isWarnEnabled();
    }

    @Override
    public void warn(String format, Object... args) {
        if (isWarnEnabled()) {
            this.logger.warn(format, args);
        }
    }

    @Override
    public boolean isErrorEnabled() {
        return this.logger.isErrorEnabled();
    }

    @Override
    public void error(String format, Object... args) {
        if (isErrorEnabled()) {
            this.logger.error(format, args);
        }
    }
}
