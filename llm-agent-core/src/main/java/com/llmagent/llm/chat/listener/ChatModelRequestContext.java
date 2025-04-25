package com.llmagent.llm.chat.listener;

import com.llmagent.Experimental;

import java.util.Map;

import com.llmagent.llm.ModelProvider;
import com.llmagent.llm.chat.ChatLanguageModel;
import com.llmagent.llm.chat.request.ChatRequest;

/**
 * The request context. It contains the {@link ChatRequest} and attributes.
 * The attributes can be used to pass data between methods of a {@link ChatModelListener}
 * or between multiple {@link ChatModelListener}s.
 */
@Experimental
public class ChatModelRequestContext {

    private final ChatRequest request;
    private final ModelProvider modelProvider;
    private final Map<Object, Object> attributes;

    public ChatModelRequestContext(ChatRequest request,
                                   ModelProvider modelProvider,
                                   Map<Object, Object> attributes) {
        this.request = request;
        this.modelProvider = modelProvider;
        this.attributes = attributes;
    }

    /**
     * @return The request to the {@link ChatLanguageModel}.
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
