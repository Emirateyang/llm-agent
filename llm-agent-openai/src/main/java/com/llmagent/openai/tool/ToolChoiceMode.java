package com.llmagent.openai.tool;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ToolChoiceMode {

    @JsonProperty("none")
    NONE,
    @JsonProperty("auto")
    AUTO,
    @JsonProperty("required")
    REQUIRED
}
