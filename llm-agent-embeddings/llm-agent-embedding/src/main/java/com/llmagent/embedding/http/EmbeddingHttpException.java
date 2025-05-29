package com.llmagent.embedding.http;

public class EmbeddingHttpException extends RuntimeException {

    private final int code;

    public EmbeddingHttpException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int code() {
        return code;
    }
}
