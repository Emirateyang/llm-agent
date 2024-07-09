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

import com.llmagent.data.Metadata;
import com.llmagent.data.segment.TextSegment;

import java.util.Objects;

import static com.llmagent.util.StringUtil.quoted;

public class Document {
    /**
     * Common metadata key for the name of the file from which the document was loaded.
     */
    public static final String FILE_NAME = "file_name";
    /**
     * Common metadata key for the absolute path of the directory from which the document was loaded.
     */
    public static final String ABSOLUTE_DIRECTORY_PATH = "absolute_directory_path";
    /**
     * Common metadata key for the URL from which the document was loaded.
     */
    public static final String URL = "url";

    private final String text;
    private final Metadata metadata;

    /**
     * Creates a new Document from the given text.
     *
     * <p>The created document will have empty metadata.
     *
     * @param text the text of the document.
     */
    public Document(String text) {
        this(text, new Metadata());
    }

    /**
     * Creates a new Document from the given text.
     *
     * @param text     the text of the document.
     * @param metadata the metadata of the document.
     */
    public Document(String text, Metadata metadata) {
        this.text = text;
        this.metadata = metadata;
    }

    /**
     * Returns the text of this document.
     *
     * @return the text.
     */
    public String text() {
        return text;
    }

    /**
     * Returns the metadata associated with this document.
     *
     * @return the metadata.
     */
    public Metadata metadata() {
        return metadata;
    }

    /**
     * Looks up the metadata value for the given key.
     *
     * @param key the key to look up.
     * @return the metadata value for the given key, or null if the key is not present.
     * @deprecated as of 0.31.0, use {@link #metadata()} and then {@link Metadata#getString(String)},
     * {@link Metadata#getInteger(String)}, {@link Metadata#getLong(String)}, {@link Metadata#getFloat(String)},
     * {@link Metadata#getDouble(String)} instead.
     */
    @Deprecated
    public String metadata(String key) {
        return metadata.get(key);
    }

    /**
     * Builds a TextSegment from this document.
     *
     * @return a TextSegment.
     */
    public TextSegment toTextSegment() {
        return TextSegment.from(text, metadata.copy().put("index", "0"));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Document that = (Document) o;
        return Objects.equals(this.text, that.text)
                && Objects.equals(this.metadata, that.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text, metadata);
    }

    @Override
    public String toString() {
        return "Document {" +
                " text = " + quoted(text) +
                " metadata = " + metadata.toMap() +
                " }";
    }

    /**
     * Creates a new Document from the given text.
     *
     * <p>The created document will have empty metadata.</p>
     *
     * @param text the text of the document.
     * @return a new Document.
     */
    public static Document from(String text) {
        return new Document(text);
    }

    /**
     * Creates a new Document from the given text.
     *
     * @param text     the text of the document.
     * @param metadata the metadata of the document.
     * @return a new Document.
     */
    public static Document from(String text, Metadata metadata) {
        return new Document(text, metadata);
    }

    /**
     * Creates a new Document from the given text.
     *
     * <p>The created document will have empty metadata.</p>
     *
     * @param text the text of the document.
     * @return a new Document.
     */
    public static Document document(String text) {
        return from(text);
    }

    /**
     * Creates a new Document from the given text.
     *
     * @param text     the text of the document.
     * @param metadata the metadata of the document.
     * @return a new Document.
     */
    public static Document document(String text, Metadata metadata) {
        return from(text, metadata);
    }
}
