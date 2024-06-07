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
package com.llmagent.vector.store;

import com.llmagent.data.Metadata;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class VectorData extends Metadata {

    private List<Double> embedding = new ArrayList<>();

    public List<Double> getEmbedding() {
        return embedding;
    }

    public void setEmbedding(List<Double> embedding) {
        this.embedding = embedding;
    }

    @Override
    public String toString() {
        return "VectorData{" +
                "embedding=" + embedding.stream().map(String::valueOf).collect(Collectors.joining(",")) +
                ", metaDataMap=" + metadata +
                '}';
    }
}
