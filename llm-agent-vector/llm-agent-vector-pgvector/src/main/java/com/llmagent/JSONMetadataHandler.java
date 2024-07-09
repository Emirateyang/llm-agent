package com.llmagent;

import com.llmagent.data.Metadata;
import com.llmagent.util.JsonUtil;
import com.llmagent.util.ValidationUtil;
import com.llmagent.vector.store.filter.Filter;

import java.sql.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class JSONMetadataHandler implements MetadataHandler {

    final MetadataColumDefinition columnDefinition;
    final String columnName;
    final JSONFilterMapper filterMapper;
    final List<String> indexes;


    public JSONMetadataHandler(MetadataStorageConfig config) {
        List<String> definition = ValidationUtil.ensureNotEmpty(config.columnDefinitions(), "Metadata definition");
        if (definition.size() > 1) {
            throw new IllegalArgumentException("Metadata definition should be an unique column definition, " +
                    "example: metadata JSON NULL");
        }
        this.columnDefinition = MetadataColumDefinition.from(definition.get(0));
        this.columnName = this.columnDefinition.getName();
        this.filterMapper = new JSONFilterMapper(columnName);
        this.indexes = config.indexes() != null ? config.indexes() : Collections.emptyList();
    }

    @Override
    public String columnDefinitionsString() {
        return columnDefinition.getFullDefinition();
    }

    @Override
    public List<String> columnsNames() {
        return Collections.singletonList(this.columnName);
    }

    @Override
    public void createMetadataIndexes(Statement statement, String table) {
        if (!this.indexes.isEmpty()) {
            throw new RuntimeException("Indexes are not allowed for JSON metadata, use JSONB instead");
        }
    }

    @Override
    public String whereClause(Filter filter) {
        return filterMapper.map(filter);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Metadata fromResultSet(ResultSet resultSet) {
        try {
            String metadataJson = resultSet.getString(columnsNames().get(0)) != null? resultSet.getString(columnsNames().get(0)) : "{}";
            return new Metadata(JsonUtil.fromJson(metadataJson, Map.class));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String insertClause() {
        return String.format("%s = EXCLUDED.%s", this.columnName, this.columnName);
    }

    @Override
    public void setMetadata(PreparedStatement upsertStmt, Integer parameterInitialIndex, Metadata metadata) {
        try {
            upsertStmt.setObject(parameterInitialIndex, JsonUtil.toJson(metadata.toMap()), Types.OTHER);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
