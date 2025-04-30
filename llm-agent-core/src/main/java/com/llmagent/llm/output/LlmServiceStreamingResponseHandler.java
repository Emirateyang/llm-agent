package com.llmagent.llm.output;

import com.llmagent.data.message.AiMessage;
import com.llmagent.data.message.ChatMessage;
import com.llmagent.data.message.ToolMessage;
import com.llmagent.llm.chat.request.ChatRequest;
import com.llmagent.llm.chat.response.ChatResponse;
import com.llmagent.llm.chat.response.StreamingChatResponseHandler;
import com.llmagent.llm.service.LlmServiceContext;
import com.llmagent.llm.tool.ToolExecution;
import com.llmagent.llm.tool.ToolExecutor;
import com.llmagent.llm.tool.ToolRequest;
import com.llmagent.llm.tool.ToolSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static com.llmagent.util.ObjectUtil.copy;
import static com.llmagent.util.ValidationUtil.ensureNotNull;

public class LlmServiceStreamingResponseHandler implements StreamingChatResponseHandler {

    private final Logger log = LoggerFactory.getLogger(LlmServiceStreamingResponseHandler.class);

    private final LlmServiceContext context;
    private final Object memoryId;

    private final Consumer<String> partialResponseHandler;
    private final Consumer<ToolExecution> toolExecutionHandler;
    private final Consumer<ChatResponse> completeResponseHandler;

    private final Consumer<Throwable> errorHandler;

    private final List<ChatMessage> temporaryMemory;
    private final TokenUsage tokenUsage;

    private final List<ToolSpecification> toolSpecifications;
    private final Map<String, ToolExecutor> toolExecutors;

    LlmServiceStreamingResponseHandler(LlmServiceContext context,
                                      Object memoryId,
                                      Consumer<String> partialResponseHandler,
                                      Consumer<ToolExecution> toolExecutionHandler,
                                      Consumer<ChatResponse> completeResponseHandler,
                                      Consumer<Throwable> errorHandler,
                                      List<ChatMessage> temporaryMemory,
                                      TokenUsage tokenUsage,
                                      List<ToolSpecification> toolSpecifications,
                                      Map<String, ToolExecutor> toolExecutors) {
        this.context = ensureNotNull(context, "context");
        this.memoryId = ensureNotNull(memoryId, "memoryId");

        this.partialResponseHandler = ensureNotNull(partialResponseHandler, "partialResponseHandler");
        this.completeResponseHandler = completeResponseHandler;
        this.toolExecutionHandler = toolExecutionHandler;
        this.errorHandler = errorHandler;

        this.temporaryMemory = new ArrayList<>(temporaryMemory);
        this.tokenUsage = ensureNotNull(tokenUsage, "tokenUsage");

        this.toolSpecifications = copy(toolSpecifications);
        this.toolExecutors = copy(toolExecutors);
    }

    @Override
    public void onPartialResponse(String partialResponse) {
        partialResponseHandler.accept(partialResponse);
    }

    @Override
    public void onCompleteResponse(ChatResponse completeResponse) {

        AiMessage aiMessage = completeResponse.aiMessage();
        addToMemory(aiMessage);

        if (aiMessage.hasToolRequests()) {
            for (ToolRequest toolExecutionRequest : aiMessage.toolRequests()) {
                String toolName = toolExecutionRequest.name();
                ToolExecutor toolExecutor = toolExecutors.get(toolName);
                String toolExecutionResult = toolExecutor.execute(toolExecutionRequest, memoryId);
                ToolMessage toolExecutionResultMessage = ToolMessage.from(
                        toolExecutionRequest,
                        toolExecutionResult
                );
                addToMemory(toolExecutionResultMessage);

                if (toolExecutionHandler != null) {
                    ToolExecution toolExecution = ToolExecution.builder()
                            .request(toolExecutionRequest)
                            .result(toolExecutionResult)
                            .build();
                    toolExecutionHandler.accept(toolExecution);
                }
            }

            ChatRequest chatRequest = ChatRequest.builder()
                    .messages(messagesToSend(memoryId))
                    .toolSpecifications(toolSpecifications)
                    .build();

            StreamingChatResponseHandler handler = new LlmServiceStreamingResponseHandler(
                    context,
                    memoryId,
                    partialResponseHandler,
                    toolExecutionHandler,
                    completeResponseHandler,
                    errorHandler,
                    temporaryMemory,
                    TokenUsage.sum(tokenUsage, completeResponse.metadata().tokenUsage()),
                    toolSpecifications,
                    toolExecutors
            );

            context.streamingChatModel.chat(chatRequest, handler);
        } else {
            if (completeResponseHandler != null) {
                ChatResponse finalChatResponse = ChatResponse.builder()
                        .aiMessage(aiMessage)
                        .metadata(completeResponse.metadata().toBuilder()
                                .tokenUsage(tokenUsage.add(completeResponse.metadata().tokenUsage()))
                                .build())
                        .build();
                completeResponseHandler.accept(finalChatResponse);
            }
        }
    }

    private void addToMemory(ChatMessage chatMessage) {
        if (context.hasChatMemory()) {
            context.chatMemoryService.getOrCreateChatMemory(memoryId).add(chatMessage);
        } else {
            temporaryMemory.add(chatMessage);
        }
    }

    private List<ChatMessage> messagesToSend(Object memoryId) {
        return context.hasChatMemory()
                ? context.chatMemoryService.getOrCreateChatMemory(memoryId).messages()
                : temporaryMemory;
    }

    @Override
    public void onError(Throwable error) {
        if (errorHandler != null) {
            try {
                errorHandler.accept(error);
            } catch (Exception e) {
                log.error("While handling the following error...", error);
                log.error("...the following error happened", e);
            }
        } else {
            log.warn("Ignored error", error);
        }
    }
}
