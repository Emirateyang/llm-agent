/*
 *  Copyright (c) 2023-2025, llm-agent (emirate.yang@gmail.com).
 *  <p>
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  <p>
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  <p>
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.llmagent.llm.embedding;

import com.llmagent.data.segment.TextSegment;
import com.llmagent.llm.output.LlmResponse;
import com.llmagent.vector.store.VectorData;

import java.util.Collections;
import java.util.List;

/**
 * Represents a model that can convert a given text into an embedding (vector representation of the text).
 */
public interface EmbeddingModel {

    /**
     * Embed a text.
     *
     * @param text the text to embed.
     * @return the embedding.
     */
    default LlmResponse<VectorData> embed(String text) {
        return embed(TextSegment.from(text));
    }

    /**
     * Embed the text content of a TextSegment.
     *
     * @param textSegment the text segment to embed.
     * @return the embedding.
     */
    default LlmResponse<VectorData> embed(TextSegment textSegment) {
        LlmResponse<List<VectorData>> response = embedAll(Collections.singletonList(textSegment));
        return LlmResponse.from(response.content().get(0), response.tokenUsage(), response.finishReason());
    }

    /**
     * Embeds the text content of a list of TextSegments.
     *
     * @param documents the text segments to embed.
     * @return the embeddings.
     */
    LlmResponse<List<VectorData>> embedAll(List<TextSegment> documents);

    /**
     * Returns the dimension of the {@link VectorData} produced by this embedding model.
     *
     * @return dimension of the embedding
     */
    default int dimension() {
        return embed("test").content().dimension();
    }
}
