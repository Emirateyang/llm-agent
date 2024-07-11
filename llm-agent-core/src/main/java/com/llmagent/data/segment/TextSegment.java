package com.llmagent.data.segment;

import com.llmagent.data.Metadata;

import java.util.Objects;

import static com.llmagent.util.StringUtil.quoted;

public class TextSegment {

    // segment id
    private String sid;
    private final String text;
    private final Metadata metadata;

    private Metadata bizData;

    /**
     * Creates a new text segment.
     *
     * @param text     the text.
     * @param metadata the metadata.
     */
    public TextSegment(String text, Metadata metadata, Metadata bizData) {
        this.text = text;
        this.metadata = metadata;
        this.bizData = bizData;
    }

    public TextSegment(String sid, String text, Metadata metadata) {
        this.sid = sid;
        this.text = text;
        this.metadata = metadata;
    }

    public TextSegment(String sid, String text, Metadata metadata, Metadata bizData) {
        this.sid = sid;
        this.text = text;
        this.metadata = metadata;
        this.bizData = bizData;
    }

    public String sid() {
        return sid;
    }

    /**
     * Returns the text.
     *
     * @return the text.
     */
    public String text() {
        return text;
    }

    /**
     * Returns the metadata.
     *
     * @return the metadata.
     */
    public Metadata metadata() {
        return metadata;
    }

    /**
     * Returns the metadata value for the given key.
     *
     * @param key the key.
     * @return the metadata value, or null if not found.
     * @deprecated as of 0.31.0, use {@link #metadata()} and then {@link Metadata#getString(String)},
     * {@link Metadata#getInteger(String)}, {@link Metadata#getLong(String)}, {@link Metadata#getFloat(String)},
     * {@link Metadata#getDouble(String)} instead.
     */
    @Deprecated
    public String metadata(String key) {
        return metadata.get(key);
    }

    public Metadata bizData() {
        return bizData;
    }

    public String bizData(String key) {
        return bizData.get(key);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TextSegment that = (TextSegment) o;
        return Objects.equals(this.text, that.text)
                && Objects.equals(this.metadata, that.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text, metadata);
    }

    @Override
    public String toString() {
        return "TextSegment {" +
                " text = " + quoted(text) +
                " metadata = " + metadata.toMap() +
                " }";
    }

    /**
     * Creates a new text segment.
     *
     * @param text the text.
     * @return the text segment.
     */
    public static TextSegment from(String text) {
        return new TextSegment(text, new Metadata(), new Metadata());
    }

    public static TextSegment from(String sid, String text) {
        return new TextSegment(sid, text, new Metadata(), new Metadata());
    }

    /**
     * Creates a new text segment.
     *
     * @param text     the text.
     * @param metadata the metadata.
     * @return the text segment.
     */
    public static TextSegment from(String text, Metadata metadata) {
        return new TextSegment(text, metadata, new Metadata());
    }

    public static TextSegment from(String text, Metadata metadata, Metadata bizData) {
        return new TextSegment(text, metadata, bizData);
    }

}
