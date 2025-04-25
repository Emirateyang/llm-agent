package com.llmagent.exception;

public class LlmAgentException extends RuntimeException {

    public LlmAgentException(String message) {
        super(message);
    }

    public LlmAgentException(Throwable cause) {
        this(cause.getMessage(), cause);
    }

    public LlmAgentException(String message, Throwable cause) {
        super(message, cause);
    }
}
