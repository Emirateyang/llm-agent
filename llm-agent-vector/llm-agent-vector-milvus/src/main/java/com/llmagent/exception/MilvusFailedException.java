package com.llmagent.exception;

public class MilvusFailedException extends RuntimeException {

    public MilvusFailedException() {
        super();
    }

    public MilvusFailedException(String message) {
        super(message);
    }

    public MilvusFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
