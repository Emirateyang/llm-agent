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
package com.llmagent.data.document;

import com.llmagent.vector.store.VectorData;

import java.util.Map;

public class Document extends VectorData {

    /**
     * Document ID
     */
    private String id;

    /**
     * Document Content
     */
    private String content;

    private double score;


    public Document() {
    }

    public Document(String content) {
        this.content = content;
    }

    public Document(String id, String content, Map<String, Object> metadata) {
        this.id = id;
        this.content = content;
        super.metadata = metadata;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public static Document of(String content){
        Document document = new Document();
        document.setContent(content);
        return document;
    }
}
