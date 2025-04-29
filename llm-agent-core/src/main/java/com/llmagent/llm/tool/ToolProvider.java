package com.llmagent.llm.tool;

import com.llmagent.data.message.UserMessage;
import com.llmagent.llm.memory.MemoryId;

/**
 * A tool provider. It is called each time the LLM service is called and supplies tools for that specific call.
 * <p>
 * Tools returned in {@link ToolProviderResult} will be included in the request to the LLM.
 **/
@FunctionalInterface
public interface ToolProvider {

    /**
     * Provides tools for the request to the LLM.
     *
     * @param request {@link ToolProviderRequest} contains {@link UserMessage} and chat memory id (see {@link MemoryId}).
     * @return {@link ToolProviderResult} contains tools that should be included in the request to the LLM.
     */
    ToolProviderResult provideTools(ToolProviderRequest request);
}
