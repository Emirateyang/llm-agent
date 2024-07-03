package com.llmagent.llm.moderation;

import com.llmagent.data.document.Document;
import com.llmagent.data.message.ChatMessage;
import com.llmagent.llm.input.Prompt;
import com.llmagent.llm.output.Response;

import java.util.List;

public interface ModerationModel {

    /**
     * Moderates the given text.
     * @param text the text to moderate.
     * @return the moderation {@code Response}.
     */
    Response<Moderation> moderate(String text);

    /**
     * Moderates the given prompt.
     * @param prompt the prompt to moderate.
     * @return the moderation {@code Response}.
     */
    default Response<Moderation> moderate(Prompt prompt) {
        return moderate(prompt.text());
    }

    /**
     * Moderates the given list of chat messages.
     * @param messages the list of chat messages to moderate.
     * @return the moderation {@code Response}.
     */
    Response<Moderation> moderate(List<ChatMessage> messages);

    /**
     * Moderates the given document.
     * @param document the document to moderate.
     * @return the moderation {@code Response}.
     */
    default Response<Moderation> moderate(Document document) {
        return moderate(document.getContent());
    }
}
