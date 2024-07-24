package com.llmagent.dify.exception;

public class DifyHttpException extends RuntimeException {

    private final int code;

    public DifyHttpException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int code() {
        return code;
    }
}
