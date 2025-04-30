package com.llmagent.llm.service;

import com.llmagent.llm.memory.ChatMemory;

public interface ChatMemoryAccess {
    /**
     * Returns the {@link ChatMemory} with the given id for this AI service or null if such memory doesn't exist.
     *
     * @param memoryId The id of the chat memory.
     * @return The {@link ChatMemory} with the given memoryId or null if such memory doesn't exist.
     */
    ChatMemory getChatMemory(Object memoryId);

    /**
     * Evicts the {@link ChatMemory} with the given id.
     *
     * @param memoryId The id of the chat memory to be evicted.
     * @return true if {@link ChatMemory} with the given id existed, and it was successfully evicted, false otherwise.
     */
    boolean evictChatMemory(Object memoryId);
}
