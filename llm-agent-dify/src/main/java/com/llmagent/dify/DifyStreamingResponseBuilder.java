package com.llmagent.dify;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.llmagent.data.message.AiMessage;
import com.llmagent.dify.chat.DifyChatResponseMetadata;
import com.llmagent.dify.chat.DifyStreamingChatCompletionResponse;
import com.llmagent.dify.chat.DifyUsage;
import com.llmagent.dify.chat.RetrieverResource;
import com.llmagent.llm.chat.response.ChatResponse;
import com.llmagent.llm.output.LlmResponse;
import com.llmagent.llm.output.RetrieverResources;
import com.llmagent.llm.output.TokenUsage;
import com.llmagent.llm.tool.ToolRequest;
import com.llmagent.util.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import static com.llmagent.util.StringUtil.isNullOrBlank;
import static java.util.stream.Collectors.toList;

/**
 * This class needs to be thread safe because it is called when a streaming result comes back
 * and there is no guarantee that this thread will be the same as the one that initiated the request,
 * in fact it almost certainly won't be.
 */
public class DifyStreamingResponseBuilder {

    private final StringBuffer contentBuilder = new StringBuffer();

    private final Map<String, ToolRequestBuilder> indexToToolExecutionRequestBuilder = new ConcurrentHashMap<>();

    private final AtomicReference<String> id = new AtomicReference<>();
    private final AtomicReference<TokenUsage> tokenUsage = new AtomicReference<>();
    private final AtomicReference<String> conversationId = new AtomicReference<>();
    private List<RetrieverResource> retrieverResources;


    public DifyStreamingResponseBuilder() {
    }

    public void append(DifyStreamingChatCompletionResponse partialResponse) {
        if (partialResponse == null) {
            return;
        }

        if (!isNullOrBlank(partialResponse.id())) {
            this.id.set(partialResponse.id());
        }

        String event = partialResponse.getEvent();
        String tool = partialResponse.getTool();
        String toolInput = partialResponse.getToolInput();
        String answer = partialResponse.getAnswer();

        if (!"message_end".equals(event) && StringUtil.noText(answer) && StringUtil.noText(tool) && StringUtil.noText(toolInput)) {
            return;
        }

        if ("message_end".equals(event)) {
            this.conversationId.set(partialResponse.getConversationId());
            DifyUsage usage = partialResponse.getMetadata().getUsage();
            this.tokenUsage.set(tokenUsageFrom(usage));
            if (partialResponse.getMetadata().getRetrieverResources() != null) {
                retrieverResources = partialResponse.getMetadata().getRetrieverResources();
            }
            return;
        }

        if (StringUtil.hasText(answer)) {
            contentBuilder.append(answer);
            return;
        }

        if (StringUtil.hasText(tool) && StringUtil.hasText(toolInput)) {
            String observation = partialResponse.getObservation();
            if (!StringUtil.noText(observation)) {
                String[] tools = tool.split(";");
                JsonObject jsonObject = JsonParser.parseString(toolInput).getAsJsonObject();
                for (String t : tools) {
                    ToolRequestBuilder toolRequestBuilder
                            = indexToToolExecutionRequestBuilder.computeIfAbsent(t, idx -> new ToolRequestBuilder());
                    toolRequestBuilder.nameBuilder.append(t);
                    toolRequestBuilder.argumentsBuilder.append(jsonObject.getAsJsonObject(t).toString());
                    toolRequestBuilder.observationBuilder.append(partialResponse.getObservation());
                }
                this.conversationId.set(partialResponse.getConversationId());
            }
        }
    }

//    public LlmResponse<AiMessage> build() {
//        List<ToolRequest> toolRequests = null;
//        if (!indexToToolExecutionRequestBuilder.isEmpty()) {
//            toolRequests = indexToToolExecutionRequestBuilder.values().stream()
//                    .map(it -> {
//                                ToolRequest toolRequest = ToolRequest.builder()
//                                .id(it.idBuilder.toString())
//                                .name(it.nameBuilder.toString())
//                                .arguments(it.argumentsBuilder.toString())
//                                .build();
//                                toolRequest.setObservation(it.observationBuilder.toString());
//                                return toolRequest;
//                    })
//                    .collect(toList());
//        }
//
//        String content = contentBuilder.toString();
//        if (StringUtil.hasText(content) && toolRequests == null) {
//            AiMessage aiMessage = AiMessage.from(content);
//            aiMessage.setConversationId(conversationId);
//            return LlmResponse.from(aiMessage,
//                    tokenUsageFrom(usage),
//                    retrieverResourceFrom(retrieverResources));
//        } else if (StringUtil.hasText(content) && toolRequests != null) {
//            AiMessage aiMessage = AiMessage.from(content, toolRequests);
//            aiMessage.setConversationId(conversationId);
//            return LlmResponse.from(aiMessage,
//                    tokenUsageFrom(usage),
//                    retrieverResourceFrom(retrieverResources));
//        } else if (StringUtil.noText(content) && toolRequests != null) {
//            AiMessage aiMessage = AiMessage.from(toolRequests);
//            aiMessage.setConversationId(conversationId);
//            return LlmResponse.from(aiMessage,
//                    tokenUsageFrom(usage),
//                    retrieverResourceFrom(retrieverResources));
//        }
//
//        return null;
//    }

    public ChatResponse build() {
        DifyChatResponseMetadata chatResponseMetadata = DifyChatResponseMetadata.builder()
                .id(id.get())
                .tokenUsage(tokenUsage.get())
                .conversationId(conversationId.get())
                .retrieverResources(retrieverResourceFrom(retrieverResources))
                .build();

        List<ToolRequest> toolRequests = null;
        if (!indexToToolExecutionRequestBuilder.isEmpty()) {
            toolRequests = indexToToolExecutionRequestBuilder.values().stream()
                    .map(it -> {
                        ToolRequest toolRequest = ToolRequest.builder()
                                .id(it.idBuilder.toString())
                                .name(it.nameBuilder.toString())
                                .arguments(it.argumentsBuilder.toString())
                                .build();
                        toolRequest.setObservation(it.observationBuilder.toString());
                        return toolRequest;
                    })
                    .collect(toList());
        }

        String content = contentBuilder.toString();
        if (StringUtil.hasText(content) && toolRequests == null) {
            AiMessage aiMessage = AiMessage.from(content);
            return ChatResponse.builder()
                    .aiMessage(aiMessage)
                    .metadata(chatResponseMetadata)
                    .build();
        } else if (StringUtil.hasText(content) && toolRequests != null) {
            AiMessage aiMessage = AiMessage.from(content, toolRequests);
            return ChatResponse.builder()
                    .aiMessage(aiMessage)
                    .metadata(chatResponseMetadata)
                    .build();
        } else if (StringUtil.noText(content) && toolRequests != null) {
            AiMessage aiMessage = AiMessage.from(toolRequests);
            return ChatResponse.builder()
                    .aiMessage(aiMessage)
                    .metadata(chatResponseMetadata)
                    .build();
        }

        return null;
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

    private static class ToolRequestBuilder {

        private final StringBuffer idBuilder = new StringBuffer();
        private final StringBuffer nameBuilder = new StringBuffer();
        private final StringBuffer argumentsBuilder = new StringBuffer();
        private final StringBuffer observationBuilder = new StringBuffer();
    }
}
