package com.llmagent.llm.rag;

import com.llmagent.data.message.ChatMessage;
import com.llmagent.data.message.Content;
import com.llmagent.llm.chat.ChatLanguageModel;

/**
 * Augments the provided {@link ChatMessage} with retrieved {@link Content}s.
 * <br>
 * This serves as an entry point into the RAG flow in LLM Agent.
 * <br>
 *
 */
public interface RetrievalAugmentor {
    /**
     * Augments the {@link ChatLanguageModel} provided in the {@link AugmentationRequest} with retrieved {@link Content}s.
     *
     * @param augmentationRequest The {@code AugmentationRequest} containing the {@code ChatMessage} to augment.
     * @return The {@link AugmentationResult} containing the augmented {@code ChatMessage}.
     */
    AugmentationResult augment(AugmentationRequest augmentationRequest);
}
