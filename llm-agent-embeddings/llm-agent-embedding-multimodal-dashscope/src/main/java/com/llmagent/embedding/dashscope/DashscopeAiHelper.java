package com.llmagent.embedding.dashscope;

import com.llmagent.llm.output.MultimodalTokenUsage;

public class DashscopeAiHelper {
    public static final String MULTI_MODAL_API_URL = "https://dashscope.aliyuncs.com/api/v1/";

    public static MultimodalTokenUsage tokenUsageFrom(Usage usage) {
        if (usage == null) {
            return null;
        }
        return MultimodalTokenUsage.builder()
                .inputTokens(usage.inputTokens())
                .totalTokens(usage.inputTokens())
                .duration(usage.duration()).build();
    }

}
