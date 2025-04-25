package com.llmagent.exception;

public class NonRetriableException extends LlmAgentException {
    public NonRetriableException(String message) {
        super(message);
    }

    public NonRetriableException(Throwable cause) {
        this(cause.getMessage(), cause);
    }

    public NonRetriableException(String message, Throwable cause) {
        super(message, cause);
    }
}
