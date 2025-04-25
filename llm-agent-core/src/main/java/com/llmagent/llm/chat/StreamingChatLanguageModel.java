package com.llmagent.llm.chat;

import com.llmagent.data.message.ChatMessage;
import com.llmagent.data.message.UserMessage;
import com.llmagent.llm.ModelProvider;
import com.llmagent.llm.chat.listener.ChatModelListener;
import com.llmagent.llm.chat.request.ChatRequest;
import com.llmagent.llm.chat.request.ChatRequestParameters;
import com.llmagent.llm.chat.response.ChatResponse;
import com.llmagent.llm.chat.response.StreamingChatResponseHandler;
import com.llmagent.util.ChatModelListenerUtil;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.llmagent.llm.ModelProvider.OTHER;
import static com.llmagent.util.ChatModelListenerUtil.onRequest;
import static com.llmagent.util.ChatModelListenerUtil.onResponse;

/**
 * Represents a language model that has a chat interface and can stream a response one token at a time.
 */
public interface StreamingChatLanguageModel {

//    /**
//     * Generates a response from the model based on a message from a user.
//     *
//     * @param userMessage The message from the user.
//     * @param handler     The handler for streaming the response.
//     */
//    @Deprecated
//    default void generate(String userMessage, StreamingResponseHandler<AiMessage> handler) {
//        generate(Collections.singletonList(UserMessage.from(userMessage)), handler);
//    }
//
//    /**
//     * Generates a response from the model based on a message from a user.
//     *
//     * @param userMessage The message from the user.
//     * @param handler     The handler for streaming the response.
//     */
//    @Deprecated
//    default void generate(UserMessage userMessage, StreamingResponseHandler<AiMessage> handler) {
//        generate(Collections.singletonList(userMessage), handler);
//    }
//
//    /**
//     * Generates a response from the model based on a sequence of messages.
//     * Typically, the sequence contains messages in the following order:
//     * System (optional) - User - AI - User - AI - User ...
//     *
//     * @param messages A list of messages.
//     * @param handler  The handler for streaming the response.
//     */
//    @Deprecated
//    void generate(List<ChatMessage> messages, StreamingResponseHandler<AiMessage> handler);
//
//    /**
//     * Generates a response from the model based on a list of messages and a list of tool specifications.
//     * The response may either be a text message or a request to execute one of the specified tools.
//     * Typically, the list contains messages in the following order:
//     * System (optional) - User - AI - User - AI - User ...
//     *
//     * @param messages           A list of messages.
//     * @param toolSpecifications A list of tools that the model is allowed to execute.
//     *                           The model autonomously decides whether to use any of these tools.
//     * @param handler            The handler for streaming the response.
//     *                           {@link AiMessage} can contain either a textual response or a request to execute one of the tools.
//     */
//    @Deprecated
//    default void generate(List<ChatMessage> messages, List<ToolSpecification> toolSpecifications, StreamingResponseHandler<AiMessage> handler) {
//        throw new IllegalArgumentException("Tools are currently not supported by this model");
//    }
//
//    /**
//     * Generates a response from the model based on a list of messages and a tool specification.
//     *
//     * @param messages          A list of messages.
//     * @param toolSpecification A tool that the model is allowed to execute.
//     * @param handler           The handler for streaming the response.
//     */
//    @Deprecated
//    default void generate(List<ChatMessage> messages, ToolSpecification toolSpecification, StreamingResponseHandler<AiMessage> handler) {
//        throw new IllegalArgumentException("Tools are currently not supported by this model");
//    }

    /**
     * This is the main API to interact with the chat model.
     * The old API generate() has been removed.
     *
     * @param chatRequest a {@link ChatRequest}, containing all the inputs to the LLM
     * @param handler     a {@link StreamingChatResponseHandler} that will handle streaming response from the LLM
     */
    default void chat(ChatRequest chatRequest, StreamingChatResponseHandler handler) {

        ChatRequest finalChatRequest = ChatRequest.builder()
                .messages(chatRequest.messages())
                .parameters(defaultRequestParameters().overrideWith(chatRequest.parameters()))
                .build();

        List<ChatModelListener> listeners = listeners();
        Map<Object, Object> attributes = new ConcurrentHashMap<>();

        StreamingChatResponseHandler observingHandler = new StreamingChatResponseHandler() {

            @Override
            public void onPartialResponse(String partialResponse) {
                handler.onPartialResponse(partialResponse);
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                onResponse(completeResponse, finalChatRequest, provider(), attributes, listeners);
                handler.onCompleteResponse(completeResponse);
            }

            @Override
            public void onError(Throwable error) {
                ChatModelListenerUtil.onError(error, finalChatRequest, provider(), attributes, listeners);
                handler.onError(error);
            }
        };

        onRequest(finalChatRequest, provider(), attributes, listeners);
        doChat(finalChatRequest, observingHandler);
    }

    /**
     * must be implemented by all {@link StreamingChatLanguageModel} implementations
     */
    default void doChat(ChatRequest chatRequest, StreamingChatResponseHandler handler) {
        throw new RuntimeException("Not implemented");
    }

    default void chat(String userMessage, StreamingChatResponseHandler handler) {

        ChatRequest chatRequest = ChatRequest.builder()
                .messages(UserMessage.from(userMessage))
                .build();
        chat(chatRequest, handler);
    }

    default ChatRequestParameters defaultRequestParameters() {
        return ChatRequestParameters.builder().build();
    }

    default void chat(List<ChatMessage> messages, StreamingChatResponseHandler handler) {
        ChatRequest chatRequest = ChatRequest.builder()
                .messages(messages)
                .build();
        chat(chatRequest, handler);
    }

    default List<ChatModelListener> listeners() {
        return Collections.emptyList();
    }

    default ModelProvider provider() {
        return OTHER;
    }
}
