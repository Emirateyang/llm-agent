package com.llmagent.llm.service;

import com.llmagent.data.message.AiMessage;
import com.llmagent.data.message.ChatMessage;
import com.llmagent.data.message.ToolMessage;
import com.llmagent.data.message.UserMessage;
import com.llmagent.exception.IllegalConfigurationException;
import com.llmagent.llm.chat.ChatLanguageModel;
import com.llmagent.llm.chat.request.ChatRequest;
import com.llmagent.llm.chat.request.ChatRequestParameters;
import com.llmagent.llm.chat.response.ChatResponse;
import com.llmagent.llm.memory.ChatMemory;
import com.llmagent.llm.output.TokenUsage;
import com.llmagent.llm.tool.*;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;

import static com.llmagent.exception.Exceptions.runtime;
import static com.llmagent.exception.IllegalConfigurationException.illegalConfiguration;
import static com.llmagent.llm.tool.ToolSpecifications.toolSpecificationFrom;

public class ToolService {
    private final List<ToolSpecification> toolSpecifications = new ArrayList<>();
    private final Map<String, ToolExecutor> toolExecutors = new HashMap<>();
    private ToolProvider toolProvider;
    private int maxSequentialToolsInvocations = 100;

    private Function<ToolRequest, ToolMessage> toolHallucinationStrategy =
            HallucinatedToolNameStrategy.THROW_EXCEPTION;

    public void hallucinatedToolNameStrategy(
            Function<ToolRequest, ToolMessage> toolHallucinationStrategy) {
        this.toolHallucinationStrategy = toolHallucinationStrategy;
    }

    public void toolProvider(ToolProvider toolProvider) {
        this.toolProvider = toolProvider;
    }

    public void tools(Map<ToolSpecification, ToolExecutor> tools) {
        tools.forEach((toolSpecification, toolExecutor) -> {
            toolSpecifications.add(toolSpecification);
            toolExecutors.put(toolSpecification.name(), toolExecutor);
        });
    }

    public void tools(Collection<Object> objectsWithTools) {
        for (Object objectWithTool : objectsWithTools) {
            if (objectWithTool instanceof Class) {
                throw illegalConfiguration("Tool '%s' must be an object, not a class", objectWithTool);
            }

            for (Method method : objectWithTool.getClass().getDeclaredMethods()) {
                if (method.isAnnotationPresent(Tool.class)) {
                    ToolSpecification toolSpecification = toolSpecificationFrom(method);
                    if (toolExecutors.containsKey(toolSpecification.name())) {
                        throw new IllegalConfigurationException(
                                "Duplicated definition for tool: " + toolSpecification.name());
                    }
                    toolExecutors.put(toolSpecification.name(), new DefaultToolExecutor(objectWithTool, method));
                    toolSpecifications.add(toolSpecificationFrom(method));
                }
            }
        }
    }

    public void maxSequentialToolsInvocations(int maxSequentialToolsInvocations) {
        this.maxSequentialToolsInvocations = maxSequentialToolsInvocations;
    }

    public ToolServiceContext createContext(Object memoryId, UserMessage userMessage) {
        if (this.toolProvider == null) {
            return this.toolSpecifications.isEmpty() ?
                    new ToolServiceContext(null, null) :
                    new ToolServiceContext(this.toolSpecifications, this.toolExecutors);
        }

        List<ToolSpecification> toolsSpecs = new ArrayList<>(this.toolSpecifications);
        Map<String, ToolExecutor> toolExecs = new HashMap<>(this.toolExecutors);
        ToolProviderRequest toolProviderRequest = new ToolProviderRequest(memoryId, userMessage);
        ToolProviderResult toolProviderResult = toolProvider.provideTools(toolProviderRequest);
        if (toolProviderResult != null) {
            for (Map.Entry<ToolSpecification, ToolExecutor> entry :
                    toolProviderResult.tools().entrySet()) {
                if (toolExecs.putIfAbsent(entry.getKey().name(), entry.getValue()) == null) {
                    toolsSpecs.add(entry.getKey());
                } else {
                    throw new IllegalConfigurationException(
                            "Duplicated definition for tool: " + entry.getKey().name());
                }
            }
        }
        return new ToolServiceContext(toolsSpecs, toolExecs);
    }

    public ToolServiceResult executeInferenceAndToolsLoop(
            ChatResponse chatResponse,
            ChatRequestParameters parameters,
            List<ChatMessage> messages,
            ChatLanguageModel chatModel,
            ChatMemory chatMemory,
            Object memoryId,
            Map<String, ToolExecutor> toolExecutors) {
        TokenUsage tokenUsageAccumulator = chatResponse.metadata().tokenUsage();
        int executionsLeft = maxSequentialToolsInvocations;
        List<ToolExecution> toolExecutions = new ArrayList<>();

        while (true) {

            if (executionsLeft-- == 0) {
                throw runtime(
                        "Something is wrong, exceeded %s sequential tool executions", maxSequentialToolsInvocations);
            }

            AiMessage aiMessage = chatResponse.aiMessage();

            if (chatMemory != null) {
                chatMemory.add(aiMessage);
            } else {
                messages = new ArrayList<>(messages);
                messages.add(aiMessage);
            }

            if (!aiMessage.hasToolRequests()) {
                break;
            }

            for (ToolRequest toolRequest : aiMessage.toolRequests()) {
                ToolExecutor toolExecutor = toolExecutors.get(toolRequest.name());

                ToolMessage toolMessage = toolExecutor == null
                        ? applyToolHallucinationStrategy(toolRequest)
                        : ToolMessage.from(
                        toolRequest, toolExecutor.execute(toolRequest, memoryId));

                toolExecutions.add(ToolExecution.builder()
                        .request(toolRequest)
                        .result(toolMessage.content())
                        .build());

                if (chatMemory != null) {
                    chatMemory.add(toolMessage);
                } else {
                    messages.add(toolMessage);
                }
            }

            if (chatMemory != null) {
                messages = chatMemory.messages();
            }

            ChatRequest chatRequest = ChatRequest.builder()
                    .messages(messages)
                    .parameters(parameters)
                    .build();

            chatResponse = chatModel.chat(chatRequest);

            tokenUsageAccumulator = TokenUsage.sum(
                    tokenUsageAccumulator, chatResponse.metadata().tokenUsage());
        }

        chatResponse = ChatResponse.builder()
                .aiMessage(chatResponse.aiMessage())
                .metadata(chatResponse.metadata().toBuilder()
                        .tokenUsage(tokenUsageAccumulator)
                        .build())
                .build();

        return new ToolServiceResult(chatResponse, toolExecutions);
    }

    public ToolMessage applyToolHallucinationStrategy(ToolRequest toolRequest) {
        return toolHallucinationStrategy.apply(toolRequest);
    }

    public List<ToolSpecification> toolSpecifications() {
        return toolSpecifications;
    }

    public Map<String, ToolExecutor> toolExecutors() {
        return toolExecutors;
    }

    public ToolProvider toolProvider() {
        return toolProvider;
    }
}
