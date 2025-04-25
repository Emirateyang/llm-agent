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
     * Estimates the count of tokens in the given text.
     *
     * @param text the text.
     * @return the estimated count of tokens.
     */
    int estimateTokenCountInText(String text);

    /**
     * Estimates the count of tokens in the given message.
     *
     * @param message the message.
     * @return the estimated count of tokens.
     */
    int estimateTokenCountInMessage(ChatMessage message);

    /**
     * Estimates the count of tokens in the given messages.
     *
     * @param messages the messages.
     * @return the estimated count of tokens.
     */
    int estimateTokenCountInMessages(Iterable<ChatMessage> messages);
}
