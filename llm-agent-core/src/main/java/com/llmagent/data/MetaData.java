package com.llmagent.data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class MetaData implements Serializable {

    protected Map<String, Object> metaDataMap;

    public Object getMetaData(String key) {
        return metaDataMap != null ? metaDataMap.get(key) : null;
    }

    public void addMetaData(String key, Object value) {
        if (metaDataMap == null) {
            metaDataMap = new HashMap<>();
        }
        metaDataMap.put(key, value);
    }

    public void addMetadata(Map<String, Object> metaData) {
        if (metaData == null || metaData.isEmpty()) {
            return;
        }
        if (metaDataMap == null) {
            metaDataMap = new HashMap<>();
        }
        metaDataMap.putAll(metaData);
    }

    public Object removeMetaData(String key) {
        if (this.metaDataMap == null) {
            return null;
        }
        return this.metaDataMap.remove(key);
    }

    public Map<String, Object> getMetaDataMap() {
        return metaDataMap;
    }

    public void setMetaDataMap(Map<String, Object> metaData) {
        this.metaDataMap = metaData;
    }
}
