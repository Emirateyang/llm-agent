package com.llmagent.openai.completion;

public enum CompletionModelName {

    GPT_3_5_TURBO_INSTRUCT("gpt-3.5-turbo-instruct");

    private final String value;

    CompletionModelName(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
