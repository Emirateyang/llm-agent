package com.llmagent;

public class JSONFilterMapper extends PgVectorFilterMapper {
    final String metadataColumn;

    public JSONFilterMapper(String metadataColumn) {
        this.metadataColumn = metadataColumn;
    }

    String formatKey(String key, Class<?> valueType) {
        return String.format("(%s->>'%s')::%s", metadataColumn, key, SQL_TYPE_MAP.get(valueType));
    }

    String formatKeyAsString(String key) {
        return metadataColumn + "->>'" + key + "'";
    }

}
