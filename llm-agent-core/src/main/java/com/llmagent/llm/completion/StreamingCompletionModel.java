package com.llmagent.llm.completion;

import com.llmagent.data.message.AiMessage;
import com.llmagent.llm.StreamingResponseHandler;
import com.llmagent.llm.input.Prompt;

public interface StreamingCompletionModel {

    /**
     * Generates a response from the model based on a prompt.
     *
     * @param prompt  The prompt.
     * @param handler The handler for streaming the response.
     */
    void generate(String prompt, StreamingResponseHandler<AiMessage> handler);

    /**
     * Generates a response from the model based on a prompt.
     *
     * @param prompt  The prompt.
     * @param handler The handler for streaming the response.
     */
    default void generate(Prompt prompt, StreamingResponseHandler<AiMessage> handler) {
        generate(prompt.text(), handler);
    }
}
