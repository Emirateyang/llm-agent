package com.llmagent;

import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.Collections;
import java.util.List;

@Builder
@AllArgsConstructor
public class DefaultMetadataStorageConfig implements MetadataStorageConfig {

    private MetadataStorageMode storageMode;
    private List<String> columnDefinitions;
    private List<String> indexes;
    private String indexType;

    /**
     * Just for warnings ?
     */
    @SuppressWarnings("unused")
    public DefaultMetadataStorageConfig() {
    }

    /**
     * Default configuration
     *
     * @return Default configuration
     */
    public static MetadataStorageConfig defaultConfig() {
        return DefaultMetadataStorageConfig.builder()
                .storageMode(MetadataStorageMode.JSON)
                .columnDefinitions(Collections.singletonList("metadata JSON NULL"))
                .build();
    }

    public MetadataStorageMode storageMode() {
        return storageMode;
    }

    public List<String> columnDefinitions() {
        return columnDefinitions;
    }

    public List<String> indexes() {
        return indexes;
    }

    public String indexType() {
        return indexType;
    }
}
