package com.llmagent;

import com.llmagent.data.Metadata;
import com.llmagent.util.JsonUtil;
import com.llmagent.vector.store.filter.Filter;

import java.sql.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.llmagent.util.ValidationUtil.ensureNotNull;

/**
 * The metadata usually stored as JSON/JSONB type data.
 * It can't use index when querying if the key in the JSON/JSONB data is related to another column in different table.
 * The best way is to create normal type column and create index on it.
 * This class is used to handle this case. Now it only support one biz data column
 */
public class BizDataHandler {

    final BizDataColumDefinition columnDefinition;
    final String columnName;
    final BizDataFilterMapper filterMapper;
    final String indexType;

    public BizDataHandler(BizDataColumDefinition config, String indexType) {
        String definition = ensureNotNull(config.getFullDefinition(), "BizDataColumDefinition#fullDefinition");

        this.columnDefinition = BizDataColumDefinition.from(definition);
        this.columnName = this.columnDefinition.getName();
        this.filterMapper = new BizDataFilterMapper(columnName);
        this.indexType = indexType;
    }

    public String columnDefinitionsString() {
        return columnDefinition.getFullDefinition();
    }

    public List<String> columnsNames() {
        return Collections.singletonList(this.columnName);
    }

    public void createBizDataIndexes(Statement statement, String schema, String table) {
        String indexTypeSql = indexType == null ? "" : "USING " + indexType;
        String index = columnName;
        try {
            String indexSql = String.format("create index if not exists %s_%s on %s %s (%s)",
                    table, index, schema + "." + table, indexTypeSql, index);
            statement.executeUpdate(indexSql);
        } catch (SQLException e) {
            throw new RuntimeException(String.format("Cannot create index %s: %s", index, e));
        }
    }

    public String whereClause(Filter filter) {
        return filterMapper.map(filter);
    }

    public String insertClause() {
        return String.format("%s = EXCLUDED.%s", this.columnName, this.columnName);
    }

    public void setBizData(PreparedStatement upsertStmt, Integer parameterInitialIndex, Metadata metadata) {
        try {
            upsertStmt.setObject(parameterInitialIndex, bizDataByColumnType(this.columnDefinition.getType(), metadata));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Metadata fromResultSet(ResultSet resultSet) {
        try {
            Object result = bizDataFromResultSet(this.columnDefinition.getType(), resultSet);
            Map<String, Object> metadataMap = new HashMap<>();
            metadataMap.put(columnsNames().get(0), result);
            return new Metadata(metadataMap);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Object bizDataByColumnType(String type, Metadata metadata) {
        if (type.contains("varchar") || type.contains("char")) {
            return metadata.getString(this.columnName);
        } else if (type.contains("bigint") || type.contains("int8")) {
            return metadata.getLong(this.columnName);
        } else if (type.contains("int")) {
            return metadata.getInteger(this.columnName);
        }
        return null;
    }

    private Object bizDataFromResultSet(String type, ResultSet resultSet) throws SQLException {
        if (type.contains("varchar") || type.contains("char")) {
            return resultSet.getString(this.columnName);
        } else if (type.contains("bigint") || type.contains("int8")) {
            return resultSet.getLong(this.columnName);
        } else if (type.contains("int")) {
            return resultSet.getInt(this.columnName);
        }
        return null;
    }
}
