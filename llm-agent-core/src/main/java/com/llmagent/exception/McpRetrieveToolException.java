package com.llmagent.exception;

public class McpRetrieveToolException extends LlmAgentException {
    public McpRetrieveToolException(String message) {
        super(message);
    }

    public McpRetrieveToolException(Throwable cause) {
        this(cause.getMessage(), cause);
    }

    public McpRetrieveToolException(String message, Throwable cause) {
        super(message, cause);
    }
}
