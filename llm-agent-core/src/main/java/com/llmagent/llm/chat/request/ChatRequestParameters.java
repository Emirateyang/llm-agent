package com.llmagent.llm.chat.request;

import com.llmagent.llm.chat.response.ResponseFormat;
import com.llmagent.llm.tool.ToolSpecification;

import java.util.List;

public interface ChatRequestParameters {

    String modelName();

    Double temperature();

    Double topP();

    Integer topK();

    Double frequencyPenalty();

    Double presencePenalty();

    Integer maxOutputTokens();

    List<String> stopSequences();

    List<ToolSpecification> toolSpecifications();

    ToolChoice toolChoice();

    ResponseFormat responseFormat();

    static DefaultChatRequestParameters.Builder<?> builder() { // TODO
        return new DefaultChatRequestParameters.Builder<>();
    }

    /**
     * Creates a new {@link ChatRequestParameters} by combining the current parameters with the specified ones.
     * Values from the specified parameters override values from the current parameters when there is overlap.
     * Neither the current nor the specified {@link ChatRequestParameters} objects are modified.
     *
     * <p>Example:
     * <pre>
     * Current parameters:
     *   temperature = 1.0
     *   maxOutputTokens = 100
     *
     * Specified parameters:
     *   temperature = 0.5
     *   modelName = my-model
     *
     * Result:
     *   temperature = 0.5        // Overridden from specified
     *   maxOutputTokens = 100    // Preserved from current
     *   modelName = my-model     // Added from specified
     * </pre>
     *
     * @param parameters the parameters whose values will override the current ones
     * @return a new {@link ChatRequestParameters} instance combining both sets of parameters
     */
    ChatRequestParameters overrideWith(ChatRequestParameters parameters);
}
