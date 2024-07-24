package com.llmagent.dify.chat;

public enum ResponseMode {
    STREAMING("streaming"),
    BLOCKING("blocking");

    private final String value;

    ResponseMode(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
