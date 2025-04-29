package com.llmagent.llm.service;

import com.llmagent.data.message.ToolMessage;
import com.llmagent.llm.tool.ToolRequest;

import java.util.function.Function;

import static com.llmagent.exception.Exceptions.runtime;

public enum HallucinatedToolNameStrategy implements Function<ToolRequest, ToolMessage> {
    THROW_EXCEPTION;

    public ToolMessage apply(ToolRequest toolRequest) {
        switch (this) {
            case THROW_EXCEPTION -> {
                throw runtime(
                        "The LLM is trying to execute the '%s' tool, but no such tool exists. Most likely, it is a "
                                + "hallucination. You can override this default strategy by setting the hallucinatedToolNameStrategy on the LlmService",
                        toolRequest.name());
            }
        }
        throw new UnsupportedOperationException();
    }
}
