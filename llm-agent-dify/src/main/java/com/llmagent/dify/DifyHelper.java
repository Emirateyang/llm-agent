package com.llmagent.dify;

import com.llmagent.data.message.*;
import com.llmagent.dify.chat.*;
import com.llmagent.llm.chat.request.ChatRequest;
import com.llmagent.llm.output.RetrieverResources;
import com.llmagent.llm.output.TokenUsage;

import java.util.ArrayList;
import java.util.List;


/**
 * 将 LLM 消息转换为 Dify 消息
 */
public class DifyHelper {

    public static String toDifyMessage(List<ChatMessage> messages) {

        ChatMessage message = messages.get(0);

        if (message instanceof UserMessage userMessage) {
            return userMessage.singleText();
        }
        return null;
    }

    public static AiMessage aiMessageFrom(DifyChatCompletionResponse response) {
        AiMessage aiMessage = AiMessage.aiMessage(response.answer());
        aiMessage.setConversationId(response.getConversationId());
        return aiMessage;
    }

    public static TokenUsage tokenUsageFrom(DifyUsage difyUsage) {
        if (difyUsage == null) {
            return null;
        }
        return new TokenUsage(
                difyUsage.getPromptTokens(),
                difyUsage.getCompletionTokens(),
                difyUsage.getTotalTokens()
        );
    }

    public static List<RetrieverResources> retrieverResourceFrom(List<RetrieverResource> difyRetrieverResource) {
        if (difyRetrieverResource == null) {
            return null;
        }
        List<RetrieverResources> retrieverResources = new ArrayList<>();
        for (RetrieverResource retrieverResource : difyRetrieverResource) {
            retrieverResources.add(new RetrieverResources(
                    retrieverResource.getPosition(),
                    retrieverResource.getDatasetId(),
                    retrieverResource.getDatasetName(),
                    retrieverResource.getDocumentId(),
                    retrieverResource.getDocumentName(),
                    retrieverResource.getSegmentId(),
                    retrieverResource.getScore(),
                    retrieverResource.getContent()));
        }
        return retrieverResources;
    }

    public static DifyMessageRequest toDifyChatRequest(ChatRequest chatRequest, DifyChatRequestParameters parameters) {
        DifyMessageRequest.Builder requestBuilder = DifyMessageRequest.builder()
                .inputs(parameters.getInputs())
                .query(toDifyMessage(chatRequest.messages()))
                .responseMode(parameters.getResponseMode())
                .conversationId(parameters.getConversationId())
                .autoGenerateName(parameters.hasAutoGenerateName())
                .breakOnToolCalled(parameters.hasBreakOnToolCalled())
                .user(parameters.getUser());
        if (parameters.getFiles() != null) {
            requestBuilder.files(parameters.getFiles());
        }
        return requestBuilder.build();
    }
}
