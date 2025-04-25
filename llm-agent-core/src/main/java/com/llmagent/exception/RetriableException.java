package com.llmagent.exception;

public class RetriableException extends LlmAgentException {
    public RetriableException(String message) {
        super(message);
    }

    public RetriableException(Throwable cause) {
        this(cause.getMessage(), cause);
    }

    public RetriableException(String message, Throwable cause) {
        super(message, cause);
    }
}
