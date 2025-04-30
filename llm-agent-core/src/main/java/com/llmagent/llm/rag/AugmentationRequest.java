package com.llmagent.llm.rag;

import com.llmagent.data.Metadata;
import com.llmagent.data.message.ChatMessage;
import com.llmagent.data.message.UserMessage;

import static com.llmagent.util.ValidationUtil.ensureNotNull;

/**
 * Represents the result of a {@link ChatMessage} augmentation.
 */
public class AugmentationRequest {
    /**
     * The chat message to be augmented.
     * Currently, only {@link UserMessage} is supported.
     */
    private final ChatMessage chatMessage;

    /**
     * Additional metadata related to the augmentation request.
     */
    private final Metadata metadata;

    public AugmentationRequest(ChatMessage chatMessage, Metadata metadata) {
        this.chatMessage = ensureNotNull(chatMessage, "chatMessage");
        this.metadata = ensureNotNull(metadata, "metadata");
    }

    public ChatMessage chatMessage() {
        return chatMessage;
    }

    public Metadata metadata() {
        return metadata;
    }
}
