package com.llmagent.llm.chat;

import com.llmagent.data.document.Document;
import com.llmagent.data.message.ChatMessage;
import com.llmagent.data.message.UserMessage;
import com.llmagent.data.segment.TextSegment;
import com.llmagent.llm.input.Prompt;

import java.util.Collections;
import java.util.List;

/**
 * Represents an interface for estimating the count of tokens in various text types such as a text, message, prompt, text segment, etc.
 * This can be useful when it's necessary to know in advance the cost of processing a specified text by the LLM.
 */
public interface TokenCountEstimator {

    /**
     * Estimates the count of tokens in the specified text.
     * @param text the text
     * @return the estimated count of tokens
     */
    default int estimateTokenCount(String text) {
        return estimateTokenCount(UserMessage.userMessage(text));
    }

    /**
     * Estimates the count of tokens in the specified message.
     * @param userMessage the message
     * @return the estimated count of tokens
     */
    default int estimateTokenCount(UserMessage userMessage) {
        return estimateTokenCount(Collections.singletonList(userMessage));
    }

    /**
     * Estimates the count of tokens in the specified prompt.
     * @param prompt the prompt
     * @return the estimated count of tokens
     */
    default int estimateTokenCount(Prompt prompt) {
        return estimateTokenCount(prompt.text());
    }

    /**
     * Estimates the count of tokens in the specified document.
     * @param textSegment the document
     * @return the estimated count of tokens
     */
    default int estimateTokenCount(TextSegment textSegment) {
        return estimateTokenCount(textSegment.text());
    }

    /**
     * Estimates the count of tokens in the specified list of messages.
     * @param messages the list of messages
     * @return the estimated count of tokens
     */
    int estimateTokenCount(List<ChatMessage> messages);
}
