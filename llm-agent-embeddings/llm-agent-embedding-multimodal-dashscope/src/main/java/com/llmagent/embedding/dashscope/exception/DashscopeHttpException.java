package com.llmagent.embedding.dashscope.exception;

public class DashscopeHttpException extends RuntimeException {

    private final int code;

    public DashscopeHttpException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int code() {
        return code;
    }
}
