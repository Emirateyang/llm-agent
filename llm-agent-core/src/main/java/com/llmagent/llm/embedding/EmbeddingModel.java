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

import com.llmagent.data.document.Document;
import com.llmagent.llm.output.Response;
import com.llmagent.vector.store.VectorData;

import java.util.Collections;
import java.util.List;

/**
 * Represents a model that can convert a given text into an embedding (vector representation of the text).
 */
public interface EmbeddingModel {

    default Response<VectorData> embed(String text) {
        return embed(Document.of(text));
    }

    default Response<VectorData> embed(Document document) {
        Response<List<VectorData>> response = embedAll(Collections.singletonList(document));
        return Response.from(response.content().get(0), response.tokenUsage(), response.finishReason());
    }

    Response<List<VectorData>> embedAll(List<Document> documents);
}
