package com.llmagent.llm.chat;

import com.llmagent.data.message.AiMessage;
import com.llmagent.data.message.ChatMessage;
import com.llmagent.data.message.UserMessage;
import com.llmagent.llm.StreamingResponseHandler;
import com.llmagent.llm.tool.ToolSpecification;

import java.util.Collections;
import java.util.List;

/**
 * Represents a language model that has a chat interface and can stream a response one token at a time.
 */
public interface StreamingChatLanguageModel {

    /**
     * Generates a response from the model based on a message from a user.
     *
     * @param userMessage The message from the user.
     * @param handler     The handler for streaming the response.
     */
    default void generate(String userMessage, StreamingResponseHandler<AiMessage> handler) {
        generate(Collections.singletonList(UserMessage.from(userMessage)), handler);
    }

    /**
     * Generates a response from the model based on a message from a user.
     *
     * @param userMessage The message from the user.
     * @param handler     The handler for streaming the response.
     */
    default void generate(UserMessage userMessage, StreamingResponseHandler<AiMessage> handler) {
        generate(Collections.singletonList(userMessage), handler);
    }

    /**
     * Generates a response from the model based on a sequence of messages.
     * Typically, the sequence contains messages in the following order:
     * System (optional) - User - AI - User - AI - User ...
     *
     * @param messages A list of messages.
     * @param handler  The handler for streaming the response.
     */
    void generate(List<ChatMessage> messages, StreamingResponseHandler<AiMessage> handler);

    /**
     * Generates a response from the model based on a list of messages and a list of tool specifications.
     * The response may either be a text message or a request to execute one of the specified tools.
     * Typically, the list contains messages in the following order:
     * System (optional) - User - AI - User - AI - User ...
     *
     * @param messages           A list of messages.
     * @param toolSpecifications A list of tools that the model is allowed to execute.
     *                           The model autonomously decides whether to use any of these tools.
     * @param handler            The handler for streaming the response.
     *                           {@link AiMessage} can contain either a textual response or a request to execute one of the tools.
     */
    default void generate(List<ChatMessage> messages, List<ToolSpecification> toolSpecifications, StreamingResponseHandler<AiMessage> handler) {
        throw new IllegalArgumentException("Tools are currently not supported by this model");
    }

    /**
     * Generates a response from the model based on a list of messages and a tool specification.
     *
     * @param messages          A list of messages.
     * @param toolSpecification A tool that the model is allowed to execute.
     * @param handler           The handler for streaming the response.
     */
    default void generate(List<ChatMessage> messages, ToolSpecification toolSpecification, StreamingResponseHandler<AiMessage> handler) {
        throw new IllegalArgumentException("Tools are currently not supported by this model");
    }
}
