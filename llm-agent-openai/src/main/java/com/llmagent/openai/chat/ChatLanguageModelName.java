package com.llmagent.openai.chat;

public enum ChatLanguageModelName {
    GPT_3_5_TURBO("gpt-3.5-turbo"), // alias
    GPT_3_5_TURBO_0613("gpt-3.5-turbo-0613"),
    GPT_3_5_TURBO_1106("gpt-3.5-turbo-1106"),
    GPT_3_5_TURBO_0125("gpt-3.5-turbo-0125"),

    GPT_3_5_TURBO_16K("gpt-3.5-turbo-16k"), // alias
    GPT_3_5_TURBO_16K_0613("gpt-3.5-turbo-16k-0613"),

    GPT_4("gpt-4"), // alias
    GPT_4_0314("gpt-4-0314"),
    GPT_4_0613("gpt-4-0613"),

    GPT_4_O("gpt-4o"),

    GPT_4_O_mini("gpt-4o-mini"),

    GPT_O1_mini("o1-mini"),

    GPT_O1_preview("o1-preview"),

    GPT_4_TURBO_PREVIEW("gpt-4-turbo-preview"), // alias
    GPT_4_1106_PREVIEW("gpt-4-1106-preview"),
    GPT_4_0125_PREVIEW("gpt-4-0125-preview"),

    GPT_4_32K("gpt-4-32k"), // alias
    GPT_4_32K_0314("gpt-4-32k-0314"),
    GPT_4_32K_0613("gpt-4-32k-0613"),

    GPT_4_VISION_PREVIEW("gpt-4-vision-preview");

    private final String value;

    ChatLanguageModelName(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
