package com.gill.timewheel.log;

/**
 * LogLevel
 *
 * @author gill
 * @version 2023/11/28
 **/
public enum LogLevel {

    /**
     * ERROR
     */
    ERROR((byte)10),

    /**
     * WARN
     */
    WARN((byte)8),

    /**
     * INFO
     */
    INFO((byte)6),

    /**
     * DEBUG
     */
    DEBUG((byte)4),

    /**
     * TRACE
     */
    TRACE((byte)2);

    private final byte value;

    LogLevel(byte value) {
        this.value = value;
    }

    public byte getValue() {
        return value;
    }
}
