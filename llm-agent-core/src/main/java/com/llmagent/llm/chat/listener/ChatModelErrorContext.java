package com.llmagent.llm.chat.listener;

import com.llmagent.Experimental;
import com.llmagent.llm.chat.ChatLanguageModel;
import com.llmagent.llm.chat.StreamingChatLanguageModel;

import java.util.Map;

/**
 * The error context. It contains the error, corresponding {@link ChatModelRequest},
 * partial {@link ChatModelResponse} (if available) and attributes.
 * The attributes can be used to pass data between methods of a {@link ChatModelListener}
 * or between multiple {@link ChatModelListener}s.
 */
@Experimental
public class ChatModelErrorContext {

    private final Throwable error;
    private final ChatModelRequest request;
    private final ChatModelResponse partialResponse;
    private final Map<Object, Object> attributes;

    public ChatModelErrorContext(Throwable error,
                                 ChatModelRequest request,
                                 ChatModelResponse partialResponse,
                                 Map<Object, Object> attributes) {
        this.error = error;
        this.request = request;
        this.partialResponse = partialResponse;
        this.attributes = attributes;
    }

    /**
     * @return The error that occurred.
     */
    public Throwable error() {
        return error;
    }

    /**
     * @return The request to the {@link ChatLanguageModel} the error corresponds to.
     */
    public ChatModelRequest request() {
        return request;
    }

    /**
     * @return The partial response from the {@link ChatLanguageModel}, if available.
     * When used with {@link StreamingChatLanguageModel}, it might contain the tokens
     * that were received before the error occurred.
     */
    public ChatModelResponse partialResponse() {
        return partialResponse;
    }

    /**
     * @return The attributes map. It can be used to pass data between methods of a {@link ChatModelListener}
     * or between multiple {@link ChatModelListener}s.
     */
    public Map<Object, Object> attributes() {
        return attributes;
    }
}
