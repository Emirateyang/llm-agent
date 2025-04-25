package com.llmagent.exception;

public class HttpException extends LlmAgentException {

    private final int statusCode;

    public HttpException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public int statusCode() {
        return statusCode;
    }
}
