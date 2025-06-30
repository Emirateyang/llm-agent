package com.llmagent.embedding.doubao;

import com.llmagent.llm.output.MultimodalTokenUsage;


public class DoubaoAiHelper {
    public static final String TEXT_MODAL_API_URL = "https://ark.cn-beijing.volces.com/api/v3/";

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
