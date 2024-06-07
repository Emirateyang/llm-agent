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
package com.llmagent;

import com.llmagent.data.Metadata;
import com.llmagent.document.Document;
import com.llmagent.util.StringUtil;
import com.llmagent.util.VectorUtil;
import com.llmagent.vector.store.DocumentStore;
import com.llmagent.vector.store.SearchWrapper;
import com.llmagent.vector.store.StoreOptions;
import com.llmagent.vector.store.StoreResult;
import com.pgvector.PGvector;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.stream.IntStream;

public class PgVectorStore extends DocumentStore {

    private static final Logger logger = LoggerFactory.getLogger(PgVectorStore.class);

    private final DataSource datasource;

    private final String table;

    private final PgDistanceType distanceType;
    private final PgIndexType createIndexMethod;
    private final boolean createTable;

    private final MetadataHandler metadataHandler;
    private final String schema;

    public PgVectorStore(PgVectorConfig config, String table, PgIndexType createIndexMethod,
                         PgDistanceType distanceType,
                         MetadataStorageConfig metadataStorageConfig) {

        this.datasource = createDataSource(config.getHost(), config.getPort(), config.getUsername(),
                config.getPassword(), config.getDatabaseName());
        this.table = table;
        this.distanceType = distanceType;
        this.createIndexMethod = createIndexMethod;
        this.createTable = config.isCreateTable();
        this.schema = config.getSchemaName();

        MetadataStorageConfig storageConfig = metadataStorageConfig != null ? metadataStorageConfig : DefaultMetadataStorageConfig.defaultConfig();
        this.metadataHandler = MetadataHandlerFactory.get(storageConfig);

        initTable(config.isDropTableIfExist(), config.getIndexListSize());
    }

    private DataSource createDataSource(String host, Integer port, String user, String password, String database) {
        PGSimpleDataSource source = new PGSimpleDataSource();
        source.setServerNames(new String[]{host});
        source.setPortNumbers(new int[]{port});
        source.setDatabaseName(database);
        source.setUser(user);
        source.setPassword(password);
        return source;
    }

    protected void initTable(Boolean dropTableIfExist, Integer indexListSize) {
        String query = "init";
        String tableName = this.schema + "." + this.table;
        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            if (dropTableIfExist) {
                statement.executeUpdate(String.format("DROP TABLE IF EXISTS %s", tableName));
            }
            if (createTable) {
                query = String.format("CREATE TABLE IF NOT EXISTS %s (id varchar(64) PRIMARY KEY, " +
                                "embedding vector(%s), content TEXT NULL, %s )",
                        tableName, this.getEmbeddingModel().dimensions(),
                        metadataHandler.columnDefinitionsString());
                statement.executeUpdate(query);
                metadataHandler.createMetadataIndexes(statement, tableName);
            }
            if (createIndexMethod != PgIndexType.NONE) {
                final String indexName = tableName + "_" + createIndexMethod.name() + "_index";
                query = String.format(
                        "CREATE INDEX IF NOT EXISTS %s ON %s " +
                                "USING %s (embedding %s) " +
                                "WITH (lists = %s)",
                        indexName, tableName, createIndexMethod.name(), distanceType.index, indexListSize);
                statement.executeUpdate(query);
            }
        } catch (SQLException e) {
            throw new RuntimeException(String.format("Failed to execute '%s'", query), e);
        }
    }

    @Override
    public StoreResult addImplement(List<Document> documents, StoreOptions options) {
        String tableName = this.schema + "." + this.table;

        try (Connection connection = getConnection()) {
            String query = String.format(
                    "INSERT INTO %s (id, embedding, content, %s) VALUES (?, ?, ?, %s)" +
                            "ON CONFLICT (id) DO UPDATE SET " +
                            "embedding = EXCLUDED.embedding," +
                            "text = EXCLUDED.text," +
                            "%s;",
                    tableName, String.join(",", metadataHandler.columnsNames()),
                    String.join(",", Collections.nCopies(metadataHandler.columnsNames().size(), "?")),
                    metadataHandler.insertClause());
            try (PreparedStatement upsertStmt = connection.prepareStatement(query)) {
                for (Document doc : documents) {
                    upsertStmt.setObject(1, doc.getId());
                    upsertStmt.setObject(2, new PGvector(doc.getEmbedding()));

                    if (StringUtil.hasText(doc.getContent())) {
                        upsertStmt.setObject(3, doc.getContent());
                        metadataHandler.setMetadata(upsertStmt, 4, doc);
                    } else {
                        upsertStmt.setNull(3, Types.VARCHAR);
                        IntStream.range(4, 4 + metadataHandler.columnsNames().size()).forEach(
                                j -> {
                                    try {
                                        upsertStmt.setNull(j, Types.OTHER);
                                    } catch (SQLException e) {
                                        throw new RuntimeException(e);
                                    }
                                });
                    }
                    upsertStmt.addBatch();
                }
                upsertStmt.executeBatch();
            }
        } catch (Exception e) {
            logger.error("add document error: " + e.getMessage(), e);
            return StoreResult.fail();
//            throw new RuntimeException(e);
        }
        return StoreResult.successWithIds(documents);
    }


    @Override
    public StoreResult deleteImplement(Collection<Object> ids, StoreOptions options) {

        String tableName = this.schema + "." + this.table;

        String sql = String.format("DELETE FROM %s WHERE id = ANY (?)", tableName);
        try (Connection connection = getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)) {
            Array array = connection.createArrayOf("varchar", ids.stream().map(String::valueOf).toArray());
            statement.setArray(1, array);
            statement.executeUpdate();
        } catch (SQLException e) {
//            throw new RuntimeException(e);
            logger.error("delete document error: " + e.getMessage(), e);
            return StoreResult.fail();
        }
        return StoreResult.success();
    }

    @Override
    public List<Document> searchImplement(SearchWrapper searchWrapper, StoreOptions options) {
        String tableName = this.schema + "." + this.table;
        try (Connection connection = getConnection()) {
            String referenceVector = Arrays.toString(VectorUtil.toFloatArray(searchWrapper.getEmbedding()));
//            String whereClause = (filter == null) ? "" : metadataHandler.whereClause(filter);
            String whereClause = "";
            whereClause = (whereClause.isEmpty()) ? "" : "WHERE " + whereClause;
            String query = String.format(
                    "WITH temp AS (SELECT (2 - (embedding <=> '%s')) / 2 AS score, id, embedding, content, " +
                            "%s FROM %s %s) SELECT * FROM temp WHERE score >= %s ORDER BY score desc LIMIT %s;",
                    referenceVector, String.join(",", metadataHandler.columnsNames()),
                    tableName, whereClause, searchWrapper.getMinScore(), searchWrapper.getMaxResults());
            List<Document> documents = new ArrayList<>();
            try (PreparedStatement selectStmt = connection.prepareStatement(query)) {
                try (ResultSet resultSet = selectStmt.executeQuery()) {
                    while (resultSet.next()) {
                        Document doc = new Document();
                        doc.setScore(resultSet.getDouble("score"));
                        doc.setId(resultSet.getString("id"));

                        PGvector vector = (PGvector) resultSet.getObject("embedding");
                        doc.setEmbedding(VectorUtil.convertToVector(vector.toArray()));
                        doc.setContent(resultSet.getString("content"));
                        Metadata metadata = metadataHandler.fromResultSet(resultSet);
                        doc.setMetadata(metadata.toMap());
                        documents.add(doc);
                    }
                }
            }
            return documents;
        } catch (SQLException e) {
//            throw new RuntimeException(e);
            logger.error("Error searching in PgVector", e);
            return Collections.emptyList();
        }
    }

    protected Connection getConnection() throws SQLException {
        Connection connection = datasource.getConnection();
        // Find a way to do the following code in connection initialization.
        // Here we assume the datasource could handle a connection pool
        // and we should add the vector type on each connection
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE EXTENSION IF NOT EXISTS vector");
        }
        PGvector.addVectorType(connection);
        return connection;
    }
}
