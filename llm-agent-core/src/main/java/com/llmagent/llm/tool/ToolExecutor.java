package com.llmagent.llm.tool;

import com.llmagent.llm.memory.MemoryId;

public interface ToolExecutor {

    /**
     * Executes a tool requests.
     *
     * @param toolRequest The tool execution request. Contains tool name and arguments.
     * @param memoryId  The ID of the chat memory. See {@link MemoryId} for more details.
     * @return The result of the tool execution.
     */
    String execute(ToolRequest toolRequest, Object memoryId);
}
