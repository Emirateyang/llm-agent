package com.llmagent.data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Metadata implements Serializable {

    protected Map<String, Object> metadata;

    public Metadata() {
        this(new HashMap<>());
    }

    public Metadata(Map<String, ?> metadata) {
        this.metadata = new HashMap<>(metadata);
    }

    public Object getMetaData(String key) {
        return metadata != null ? metadata.get(key) : null;
    }

    public void addMetaData(String key, Object value) {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put(key, value);
    }

    public void addMetadata(Map<String, Object> metaData) {
        if (metaData == null || metaData.isEmpty()) {
            return;
        }
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.putAll(metaData);
    }

    public Object removeMetaData(String key) {
        if (this.metadata == null) {
            return null;
        }
        return this.metadata.remove(key);
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metaData) {
        this.metadata = metaData;
    }

    public boolean containsKey(String key) {
        return metadata.containsKey(key);
    }

    /**
     * Get a copy of the metadata as a map of key-value pairs.
     */
    public Map<String, Object> toMap() {
        return new HashMap<>(metadata);
    }
}
