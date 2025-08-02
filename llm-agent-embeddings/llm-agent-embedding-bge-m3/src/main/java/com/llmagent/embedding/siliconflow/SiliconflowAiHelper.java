package com.llmagent.embedding.siliconflow;

import com.llmagent.llm.output.MultimodalTokenUsage;


public class SiliconflowAiHelper {
    public static final String TEXT_MODAL_API_URL = "https://api.siliconflow.cn/v1/";

    public static MultimodalTokenUsage tokenUsageFrom(Usage usage) {
        if (usage == null) {
            return null;
        }
        return MultimodalTokenUsage.builder()
                .inputTokens(usage.promptTokens())
                .totalTokens(usage.totalTokens())
                .duration(0).build();
    }

}
