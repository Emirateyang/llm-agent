package com.llmagent.llm.chat.listener;

import com.llmagent.Experimental;
import com.llmagent.llm.ModelProvider;
import com.llmagent.llm.chat.ChatLanguageModel;
import com.llmagent.llm.chat.request.ChatRequest;
import com.llmagent.llm.chat.response.ChatResponse;

import java.util.Map;

/**
 * The response context. It contains {@link ChatResponse}, corresponding {@link ChatRequest} and attributes.
 * The attributes can be used to pass data between methods of a {@link ChatModelListener}
 * or between multiple {@link ChatModelListener}s.
 */
@Experimental
public class ChatModelResponseContext {

    private final ChatResponse response;
    private final ChatRequest request;
    private final ModelProvider modelProvider;
    private final Map<Object, Object> attributes;

    public ChatModelResponseContext(ChatResponse response,
                                    ChatRequest request,
                                    ModelProvider modelProvider,
                                    Map<Object, Object> attributes) {
        this.response = response;
        this.request = request;
        this.modelProvider = modelProvider;
        this.attributes = attributes;
    }

    /**
     * @return The response from the {@link ChatLanguageModel}.
     */
    public ChatResponse response() {
        return response;
    }

    /**
     * @return The request to the {@link ChatLanguageModel} the response corresponds to.
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
