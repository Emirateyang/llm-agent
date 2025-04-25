package com.llmagent.llm.chat.request;

public enum ToolChoice {
    /**
     * The chat model is free to decide whether to call tool(s).
     */
    AUTO,

    /**
     * The chat model is required to call one or more tools.
     */
    REQUIRED
}
