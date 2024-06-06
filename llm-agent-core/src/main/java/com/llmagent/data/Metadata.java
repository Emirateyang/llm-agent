package com.llmagent.data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Metadata implements Serializable {

    protected Map<String, Object> metadataMap;

    public Object getMetaData(String key) {
        return metadataMap != null ? metadataMap.get(key) : null;
    }

    public void addMetaData(String key, Object value) {
        if (metadataMap == null) {
            metadataMap = new HashMap<>();
        }
        metadataMap.put(key, value);
    }

    public void addMetadata(Map<String, Object> metaData) {
        if (metaData == null || metaData.isEmpty()) {
            return;
        }
        if (metadataMap == null) {
            metadataMap = new HashMap<>();
        }
        metadataMap.putAll(metaData);
    }

    public Object removeMetaData(String key) {
        if (this.metadataMap == null) {
            return null;
        }
        return this.metadataMap.remove(key);
    }

    public Map<String, Object> getMetadataMap() {
        return metadataMap;
    }

    public void setMetadataMap(Map<String, Object> metaData) {
        this.metadataMap = metaData;
    }
}
