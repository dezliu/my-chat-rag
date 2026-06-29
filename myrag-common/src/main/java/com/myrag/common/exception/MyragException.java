package com.myrag.common.exception;

public class MyragException extends RuntimeException {
    private final int code;

    public MyragException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
