package com.llmagent.openai.exception;

public class OpenAiHttpException extends RuntimeException {

    private final int code;

    public OpenAiHttpException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int code() {
        return code;
    }
}
