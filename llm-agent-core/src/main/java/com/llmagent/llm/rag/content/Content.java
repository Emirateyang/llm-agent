package com.llmagent.llm.rag.content;

import com.llmagent.data.segment.TextSegment;

import java.util.Map;

public interface Content {
    TextSegment textSegment();

    Map<ContentMetadata, Object> metadata();

    static Content from(String text) {
        return new DefaultContent(text);
    }

    static Content from(TextSegment textSegment) {
        return new DefaultContent(textSegment);
    }

    static Content from(TextSegment textSegment, Map<ContentMetadata, Object> metadata) {
        return new DefaultContent(textSegment, metadata);
    }
}
