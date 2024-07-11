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
package com.llmagent.data.document.splitter;

import com.llmagent.data.document.Document;
import com.llmagent.data.document.DocumentSplitter;
import com.llmagent.data.document.id.DocumentIdGenerator;
import com.llmagent.data.segment.TextSegment;
import com.llmagent.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SimpleDocumentSplitter implements DocumentSplitter {

    private final String regex;

    public SimpleDocumentSplitter(String regex) {
        this.regex = regex;
    }

    @Override
    public List<TextSegment> split(Document document, DocumentIdGenerator idGenerator) {
        if (document == null || StringUtil.noText(document.text())) {
            return Collections.emptyList();
        }
        String[] textArray = document.text().split(regex);
        List<TextSegment> texts = new ArrayList<>(textArray.length);
        for (String textString : textArray) {
            TextSegment segment = new TextSegment(idGenerator.generateId(document), textString, document.metadata());
            texts.add(segment);
        }
        return texts;
    }
}
