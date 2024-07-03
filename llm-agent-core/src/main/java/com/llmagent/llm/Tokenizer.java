package com.llmagent.llm;

import com.llmagent.data.message.ChatMessage;
import com.llmagent.llm.tool.ToolRequest;
import com.llmagent.llm.tool.ToolSpecification;

import java.util.Collections;
import java.util.List;

public interface Tokenizer {
    /**
     * Estimates the count of tokens in the given text.
     * @param text the text.
     * @return the estimated count of tokens.
     */
    int estimateTokenCountInText(String text);

    /**
     * Estimates the count of tokens in the given message.
     * @param message the message.
     * @return the estimated count of tokens.
     */
    int estimateTokenCountInMessage(ChatMessage message);

    /**
     * Estimates the count of tokens in the given messages.
     * @param messages the messages.
     * @return the estimated count of tokens.
     */
    int estimateTokenCountInMessages(Iterable<ChatMessage> messages);

    /**
     * Estimates the count of tokens in {@code Tool} annotations of the given object.
     * @param objectWithTools the object.
     * @return the estimated count of tokens.
     */
    default int estimateTokenCountInTools(Object objectWithTools) {
        return estimateTokenCountInTools(Collections.singletonList(objectWithTools));
    }

    /**
     * Estimates the count of tokens in the given tool specifications.
     * @param toolSpecifications the tool specifications.
     * @return the estimated count of tokens.
     */
    int estimateTokenCountInToolSpecifications(Iterable<ToolSpecification> toolSpecifications);

    /**
     * Estimates the count of tokens in the given tool specification.
     * @param toolSpecification the tool specification.
     * @return the estimated count of tokens.
     */
    default int estimateTokenCountInForcefulToolSpecification(ToolSpecification toolSpecification) {
        return estimateTokenCountInToolSpecifications(Collections.singletonList(toolSpecification));
    }

    /**
     * Estimates the count of tokens in the given tool execution requests.
     * @param toolRequests the tool execution request.
     * @return the estimated count of tokens.
     */
    int estimateTokenCountInToolExecutionRequests(Iterable<ToolRequest> toolRequests);

    /**
     * Estimates the count of tokens in the given tool execution request.
     * @param toolRequest the tool execution request.
     * @return the estimated count of tokens.
     */
    default int estimateTokenCountInForcefulToolExecutionRequest(ToolRequest toolRequest) {
        return estimateTokenCountInToolExecutionRequests(Collections.singletonList(toolRequest));
    }
}
