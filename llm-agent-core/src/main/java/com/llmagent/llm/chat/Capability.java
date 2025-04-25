package com.llmagent.llm.chat;

import com.llmagent.llm.chat.request.json.JsonSchema;
import com.llmagent.llm.chat.response.ResponseFormat;

public enum Capability {

    /**
     * Indicates whether {@link ChatLanguageModel} or {@link StreamingChatLanguageModel}
     * supports responding in JSON format according to the specified JSON schema.
     *
     * @see ResponseFormat
     * @see JsonSchema
     */
    RESPONSE_FORMAT_JSON_SCHEMA
}
