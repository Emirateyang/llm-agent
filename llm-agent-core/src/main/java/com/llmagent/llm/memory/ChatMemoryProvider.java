package com.llmagent.llm.memory;

/**
 * Provides instances of {@link ChatMemory}.
 */
@FunctionalInterface
public interface ChatMemoryProvider {

    /**
     * Provides an instance of {@link ChatMemory}.
     * This method is called each time an AI Service method (having a parameter annotated with {@link MemoryId})
     * is called with a previously unseen memory ID.
     * Once the {@link ChatMemory} instance is returned, it's retained in memory and managed by AI Service.
     *
     * @param memoryId The ID of the chat memory.
     * @return A {@link ChatMemory} instance.
     * @see MemoryId
     */
    ChatMemory get(Object memoryId);
}
