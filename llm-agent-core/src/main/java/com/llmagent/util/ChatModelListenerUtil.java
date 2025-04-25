package com.llmagent.util;

import com.llmagent.llm.ModelProvider;
import com.llmagent.llm.chat.listener.ChatModelErrorContext;
import com.llmagent.llm.chat.listener.ChatModelListener;
import com.llmagent.llm.chat.listener.ChatModelRequestContext;
import com.llmagent.llm.chat.listener.ChatModelResponseContext;
import com.llmagent.llm.chat.request.ChatRequest;
import com.llmagent.llm.chat.response.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class ChatModelListenerUtil {
    private static final Logger LOG = LoggerFactory.getLogger(ChatModelListenerUtil.class);

    private ChatModelListenerUtil() {
    }

    public static void onRequest(ChatRequest chatRequest,
                                 ModelProvider modelProvider,
                                 Map<Object, Object> attributes,
                                 List<ChatModelListener> listeners) {
        if (listeners == null || listeners.isEmpty()) {
            return;
        }
        ChatModelRequestContext requestContext = new ChatModelRequestContext(chatRequest, modelProvider, attributes);
        listeners.forEach(listener -> {
            try {
                listener.onRequest(requestContext);
            } catch (Exception e) {
                LOG.warn("An exception occurred during the invocation of the chat model listener. " +
                        "This exception has been ignored.", e);
            }
        });
    }

    public static void onResponse(ChatResponse chatResponse,
                                  ChatRequest chatRequest,
                                  ModelProvider modelProvider,
                                  Map<Object, Object> attributes,
                                  List<ChatModelListener> listeners) {
        if (listeners == null || listeners.isEmpty()) {
            return;
        }
        ChatModelResponseContext responseContext = new ChatModelResponseContext(
                chatResponse, chatRequest, modelProvider, attributes);
        listeners.forEach(listener -> {
            try {
                listener.onResponse(responseContext);
            } catch (Exception e) {
                LOG.warn("An exception occurred during the invocation of the chat model listener. " +
                        "This exception has been ignored.", e);
            }
        });
    }

    public static void onError(Throwable error,
                               ChatRequest chatRequest,
                               ModelProvider modelProvider,
                               Map<Object, Object> attributes,
                               List<ChatModelListener> listeners) {
        if (listeners == null || listeners.isEmpty()) {
            return;
        }
        ChatModelErrorContext errorContext = new ChatModelErrorContext(error, chatRequest, modelProvider, attributes);
        listeners.forEach(listener -> {
            try {
                listener.onError(errorContext);
            } catch (Exception e) {
                LOG.warn("An exception occurred during the invocation of the chat model listener. " +
                        "This exception has been ignored.", e);
            }
        });
    }
}
