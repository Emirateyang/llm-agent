package com.llmagent.llm.moderation;

import com.llmagent.data.message.ChatMessage;
import com.llmagent.data.segment.TextSegment;
import com.llmagent.llm.input.Prompt;
import com.llmagent.llm.output.LlmResponse;

import java.util.List;

public interface ModerationModel {

    /**
     * Moderates the given text.
     * @param text the text to moderate.
     * @return the moderation {@code Response}.
     */
    LlmResponse<Moderation> moderate(String text);

    /**
     * Moderates the given prompt.
     * @param prompt the prompt to moderate.
     * @return the moderation {@code Response}.
     */
    default LlmResponse<Moderation> moderate(Prompt prompt) {
        return moderate(prompt.text());
    }

    /**
     * Moderates the given list of chat messages.
     * @param messages the list of chat messages to moderate.
     * @return the moderation {@code Response}.
     */
    LlmResponse<Moderation> moderate(List<ChatMessage> messages);

    /**
     * Moderates the given document.
     * @param textSegment the document to moderate.
     * @return the moderation {@code Response}.
     */
    default LlmResponse<Moderation> moderate(TextSegment textSegment) {
        return moderate(textSegment.text());
    }
}
