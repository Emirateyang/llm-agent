package com.llmagent;

public class MetadataHandlerFactory {
    public MetadataHandlerFactory() {}
    /**
     * Retrieve the handler associated to the config
     * @param config MetadataConfig config
     * @return MetadataHandler
     */
    static MetadataHandler get(MetadataStorageConfig config) {
        switch(config.storageMode()) {
            case JSON:
                return new JSONMetadataHandler(config);
            case JSONB:
                return new JSONBMetadataHandler(config);
            default:
                throw new RuntimeException(String.format("Type %s not handled.", config.storageMode()));
        }
    }
}
