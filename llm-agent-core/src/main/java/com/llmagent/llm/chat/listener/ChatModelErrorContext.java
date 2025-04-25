package com.llmagent.llm.chat.listener;

import com.llmagent.Experimental;
import com.llmagent.llm.ModelProvider;
import com.llmagent.llm.chat.ChatLanguageModel;
import com.llmagent.llm.chat.StreamingChatLanguageModel;
import com.llmagent.llm.chat.request.ChatRequest;
import com.llmagent.llm.chat.response.ChatResponse;

import java.util.Map;

/**
 * The error context. It contains the error, corresponding {@link ChatRequest},
 * partial {@link ChatResponse} (if available) and attributes.
 * The attributes can be used to pass data between methods of a {@link ChatModelListener}
 * or between multiple {@link ChatModelListener}s.
 */
@Experimental
public class ChatModelErrorContext {

    private final Throwable error;
    private final ChatRequest request;
    private final ModelProvider modelProvider;
    private final Map<Object, Object> attributes;

    public ChatModelErrorContext(Throwable error,
                                 ChatRequest request,
                                 ModelProvider modelProvider,
                                 Map<Object, Object> attributes) {
        this.error = error;
        this.request = request;
        this.modelProvider = modelProvider;
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
    public ChatRequest request() {
        return request;
    }


    public ModelProvider modelProvider() {
        return modelProvider;
    }

    /**
     * @return The attributes map. It can be used to pass data between methods of a {@link ChatModelListener}
     * or between multiple {@link ChatModelListener}s.
     */
    public Map<Object, Object> attributes() {
        return attributes;
    }
}
