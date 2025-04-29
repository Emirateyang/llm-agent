package com.llmagent.llm.service;

import com.llmagent.llm.chat.ChatLanguageModel;
import com.llmagent.llm.chat.StreamingChatLanguageModel;
import com.llmagent.llm.memory.ChatMemory;
import com.llmagent.llm.memory.ChatMemoryProvider;

import java.util.Optional;
import java.util.function.Function;

public class LlmServiceContext {

    private static final Function<Object, Optional<String>> DEFAULT_MESSAGE_PROVIDER = x -> Optional.empty();

    public final Class<?> llmServiceClass;

    public ChatLanguageModel chatModel;
    public StreamingChatLanguageModel streamingChatModel;

    public ChatMemoryService chatMemoryService;
    public ToolService toolService = new ToolService();

    public Function<Object, Optional<String>> systemMessageProvider = DEFAULT_MESSAGE_PROVIDER;

    public LlmServiceContext(Class<?> llmServiceClass) {
        this.llmServiceClass = llmServiceClass;
    }

    public boolean hasChatMemory() {
        return chatMemoryService != null;
    }

    public void initChatMemories(ChatMemory chatMemory) {
        chatMemoryService = new ChatMemoryService(chatMemory);
    }

    public void initChatMemories(ChatMemoryProvider chatMemoryProvider) {
        chatMemoryService = new ChatMemoryService(chatMemoryProvider);
    }
}
