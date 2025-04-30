package com.llmagent.llm.rag.content;

import com.llmagent.data.segment.TextSegment;

import java.util.Map;
import java.util.Objects;

import static com.llmagent.util.ObjectUtil.copy;
import static com.llmagent.util.ValidationUtil.ensureNotNull;

public class DefaultContent implements Content {

    private final TextSegment textSegment;
    private final Map<ContentMetadata, Object> metadata;

    public DefaultContent(TextSegment textSegment, Map<ContentMetadata, Object> metadata) {
        this.textSegment = ensureNotNull(textSegment, "textSegment");
        this.metadata = copy(metadata);
    }

    public DefaultContent(String text) {
        this(TextSegment.from(text));
    }

    public DefaultContent(TextSegment textSegment) {
        this(textSegment, Map.of());
    }

    @Override
    public TextSegment textSegment() {
        return textSegment;
    }

    @Override
    public Map<ContentMetadata, Object> metadata() {
        return metadata;
    }

    /**
     * Compares this {@code Content} with another object for equality.
     * <br>
     * The {@code metadata} field is intentionally excluded from the equality check. Metadata is considered
     * supplementary information and does not contribute to the core identity of the {@code Content}.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Content that = (Content) o;
        return Objects.equals(this.textSegment, that.textSegment());
    }

    /**
     * Computes the hash code for this {@code Content}.
     * <br>
     * The {@code metadata} field is excluded from the hash code calculation. This ensures that two logically identical
     * {@code Content} objects with differing metadata produce the same hash code, maintaining consistent behavior in
     * hash-based collections.
     */
    @Override
    public int hashCode() {
        return Objects.hash(textSegment);
    }

    @Override
    public String toString() {
        return "DefaultContent {" +
                " textSegment = " + textSegment +
                ", metadata = " + metadata +
                " }";
    }
}
