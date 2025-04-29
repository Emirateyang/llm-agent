package com.llmagent.llm.service;

import com.llmagent.llm.memory.ChatMemory;
import com.llmagent.llm.memory.ChatMemoryProvider;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.llmagent.util.ValidationUtil.ensureNotNull;

public class ChatMemoryService {
    public static final String DEFAULT = "default";

    private ChatMemory defaultChatMemory;
    private Map<Object, ChatMemory> chatMemories;
    private ChatMemoryProvider chatMemoryProvider;

    public ChatMemoryService(ChatMemoryProvider chatMemoryProvider) {
        this.chatMemories = new ConcurrentHashMap<>();
        this.chatMemoryProvider = ensureNotNull(chatMemoryProvider, "chatMemoryProvider");
    }

    public ChatMemoryService(ChatMemory chatMemory) {
        defaultChatMemory = ensureNotNull(chatMemory, "chatMemory");
    }

    public ChatMemory getOrCreateChatMemory(Object memoryId) {
        if (memoryId == DEFAULT) {
            if (defaultChatMemory == null) {
                defaultChatMemory = chatMemoryProvider.get(DEFAULT);
            }
            return defaultChatMemory;
        }
        return chatMemories.computeIfAbsent(memoryId, chatMemoryProvider::get);
    }

    public ChatMemory getChatMemory(Object memoryId) {
        return memoryId == DEFAULT ? defaultChatMemory : chatMemories.get(memoryId);
    }

    public ChatMemory evictChatMemory(Object memoryId) {
        return chatMemories.remove(memoryId);
    }

    public void clearAll() {
        chatMemories.values().forEach(ChatMemory::clear);
        chatMemories.clear();
    }

    public Collection<Object> getChatMemoryIDs() {
        return chatMemories.keySet();
    }

    public Collection<ChatMemory> getChatMemories() {
        return chatMemories.values();
    }
}
