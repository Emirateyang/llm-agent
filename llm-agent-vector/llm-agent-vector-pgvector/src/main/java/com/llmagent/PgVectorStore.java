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
import com.llmagent.data.document.Document;
import com.llmagent.data.segment.TextSegment;
import com.llmagent.util.StringUtil;
import com.llmagent.util.VectorUtil;
import com.llmagent.vector.store.*;
import com.llmagent.vector.store.filter.Filter;
import com.pgvector.PGvector;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.llmagent.util.ObjectUtil.isNullOrEmpty;
import static com.llmagent.util.StringUtil.isNotNullOrBlank;
import static com.llmagent.util.UUIDUtil.randomUUID;
import static com.llmagent.util.ValidationUtil.*;
import static java.lang.String.join;
import static java.util.Collections.nCopies;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

@Slf4j
public class PgVectorStore implements EmbeddingStore<TextSegment> {

    private static final Logger logger = LoggerFactory.getLogger(PgVectorStore.class);

    private final DataSource datasource;

    private final String table;

    private final PgDistanceType distanceType;
    private final PgIndexType createIndexMethod;
    private final boolean needCreateTable;

    private final MetadataHandler metadataHandler;
    private final BizDataHandler bizDataHandler;
    private final String schema;

    public PgVectorStore(PgVectorConfig config, String table, PgIndexType createIndexMethod,
                         PgDistanceType distanceType,
                         MetadataStorageConfig metadataStorageConfig,
                         BizDataHandler bizDataHandler) {

        this.datasource = createDataSource(config.getHost(), config.getPort(), config.getUsername(),
                config.getPassword(), config.getDatabaseName());
        this.table = table;
        this.distanceType = distanceType;
        this.createIndexMethod = createIndexMethod;
        this.needCreateTable = config.isNeedCreateTable();
        this.schema = config.getSchemaName();

        MetadataStorageConfig storageConfig = metadataStorageConfig != null ? metadataStorageConfig : DefaultMetadataStorageConfig.defaultConfig();
        this.metadataHandler = MetadataHandlerFactory.get(storageConfig);

        this.bizDataHandler = bizDataHandler;

        int dimension = ensureGreaterThanZero(config.getDimension(), "config#dimension");
        int indexListSize = ensureGreaterThanZero(config.getIndexListSize(), "config#indexListSize");

        initTable(config.isDropTableIfExist(), indexListSize, dimension);
    }

    /**
     * Create a data source for PostgreSQL
     */
    private DataSource createDataSource(String host, Integer port, String user, String password, String database) {

        host = ensureNotBlank(host, "host");
        port = ensureGreaterThanZero(port, "port");
        user = ensureNotBlank(user, "user");
        password = ensureNotBlank(password, "password");
        database = ensureNotBlank(database, "database");

        PGSimpleDataSource source = new PGSimpleDataSource();
        source.setServerNames(new String[]{host});
        source.setPortNumbers(new int[]{port});
        source.setDatabaseName(database);
        source.setUser(user);
        source.setPassword(password);
        return source;
    }

    /**
     * Initialize the table
     */
    protected void initTable(Boolean dropTableIfExist, Integer indexListSize, Integer dimension) {

        String query = "init";
        String tableName = this.schema + "." + this.table;
        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            if (dropTableIfExist) {
                statement.executeUpdate(String.format("DROP TABLE IF EXISTS %s", tableName));
            }
            if (needCreateTable) {
                if (bizDataHandler != null && StringUtil.hasText(bizDataHandler.columnDefinitionsString())) {
                    query = String.format("CREATE TABLE IF NOT EXISTS %s (id varchar(64) PRIMARY KEY, " +
                                    "embedding vector(%s), doc_chunk TEXT NULL, %s, %s )",
                            tableName,
                            dimension,
                            bizDataHandler.columnDefinitionsString(),
                            metadataHandler.columnDefinitionsString());
                    statement.executeUpdate(query);
                    metadataHandler.createMetadataIndexes(statement, this.schema, this.table);
                    bizDataHandler.createBizDataIndexes(statement, this.schema, this.table);
                } else {
                    query = String.format("CREATE TABLE IF NOT EXISTS %s (id varchar(64) PRIMARY KEY, " +
                                    "embedding vector(%s), doc_chunk TEXT NULL, %s )",
                            tableName,
                            dimension,
                            metadataHandler.columnDefinitionsString());
                    statement.executeUpdate(query);
                    metadataHandler.createMetadataIndexes(statement, this.schema, this.table);
                }
            }
            if (createIndexMethod != PgIndexType.NONE) {
                final String indexName = this.table + "_" + createIndexMethod.name() + "_index";
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
    public String add(VectorData embedding) {
        String id = randomUUID();
        addImplement(id, embedding, null);
        return id;
    }

    @Override
    public void add(String id, VectorData embedding) {
        addImplement(id, embedding, null);
    }

    @Override
    public String add(VectorData embedding, TextSegment textSegment) {
        String id = randomUUID();
        addImplement(id, embedding, textSegment);
        return id;
    }

    @Override
    public List<String> addAll(List<VectorData> embeddings) {
        List<String> ids = embeddings.stream().map(ignored -> randomUUID()).collect(toList());
        addAllImplement(ids, embeddings, null);
        return ids;
    }

    @Override
    public List<String> addAll(List<VectorData> embeddings, List<TextSegment> embedded) {
        List<String> ids = embeddings.stream().map(ignored -> randomUUID()).collect(toList());
        addAllImplement(ids, embeddings, embedded);
        return ids;
    }

    @Override
    public void removeAll(Collection<String> ids) {
        String tableName = this.schema + "." + this.table;
        String sql = String.format("DELETE FROM %s WHERE id = ANY (?)", tableName);
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            Array array = connection.createArrayOf("string", ids.stream().map(String::valueOf).toArray());
            statement.setArray(1, array);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void removeAll(Filter filter) {
        String tableName = this.schema + "." + this.table;
        String whereClause = metadataHandler.whereClause(filter);
        String sql = String.format("DELETE FROM %s WHERE %s", tableName, whereClause);
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void removeAll() {
        String tableName = this.schema + "." + this.table;
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(String.format("TRUNCATE TABLE %s", tableName));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public EmbeddingSearchResult<TextSegment> search(EmbeddingSearchRequest request) {
        String tableName = this.schema + "." + this.table;
        VectorData referenceEmbedding = request.queryEmbedding();
        int maxResults = request.maxResults();
        double minScore = request.minScore();
        Filter filter = request.filter();
        Filter bizDataFilter = request.filter4BizData();

        List<EmbeddingMatch<TextSegment>> result = new ArrayList<>();
        try (Connection connection = getConnection()) {
            String referenceVector = Arrays.toString(referenceEmbedding.vector());
            String whereClause = (filter == null) ? "" : metadataHandler.whereClause(filter);
            whereClause = (whereClause.isEmpty()) ? "" : "WHERE " + whereClause;

            boolean hasBizData = false;
            if (bizDataHandler != null && StringUtil.hasText(bizDataHandler.columnDefinitionsString())) {
                hasBizData = true;
            }

            String query;
            if (hasBizData) {
                String bizDataClause = bizDataHandler.whereClause(bizDataFilter);
                whereClause = (whereClause.isEmpty()) ? " WHERE " + bizDataClause : " AND " + bizDataClause;
                query = String.format(
                        "WITH temp AS (SELECT (1-(embedding <=> '%s')) AS score, id, embedding, doc_chunk, " +
                                "%s, %s FROM %s %s) SELECT * FROM temp WHERE score >= %s ORDER BY score desc LIMIT %s;",
                        referenceVector, join(",", bizDataHandler.columnsNames()),
                        join(",", metadataHandler.columnsNames()), tableName, whereClause, minScore, maxResults);
            } else {
                query = String.format(
                        "WITH temp AS (SELECT (1-(embedding <=> '%s')) AS score, id, embedding, doc_chunk, " +
                                "%s FROM %s %s) SELECT * FROM temp WHERE score >= %s ORDER BY score desc LIMIT %s;",
                        referenceVector, join(",", metadataHandler.columnsNames()), tableName, whereClause, minScore, maxResults);
            }
            try (PreparedStatement selectStmt = connection.prepareStatement(query)) {
                try (ResultSet resultSet = selectStmt.executeQuery()) {
                    while (resultSet.next()) {
                        double score = resultSet.getDouble("score");
                        String embeddingId = resultSet.getString("id");

                        PGvector vector = (PGvector) resultSet.getObject("embedding");
                        VectorData embedding = new VectorData(vector.toArray());

                        String docChunk = resultSet.getString("doc_chunk");
                        TextSegment textSegment = null;
                        if (isNotNullOrBlank(docChunk)) {
                            Metadata metadata = metadataHandler.fromResultSet(resultSet);
                            if (hasBizData) {
                                Metadata bizData = bizDataHandler.fromResultSet(resultSet);
                                textSegment = TextSegment.from(docChunk, metadata, bizData);
                            } else {
                                textSegment = TextSegment.from(docChunk, metadata);
                            }
                        }
                        result.add(new EmbeddingMatch<>(score, embeddingId, embedding, textSegment));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return new EmbeddingSearchResult<>(result);
    }

    private void addImplement(String id, VectorData embedding, TextSegment embedded) {
        addAllImplement(
                singletonList(id),
                singletonList(embedding),
                embedded == null ? null : singletonList(embedded));
    }

    private void addAllImplement(List<String> ids, List<VectorData> embeddings, List<TextSegment> embedded) {
        if (isNullOrEmpty(embeddings)) {
            log.info("Empty embeddings - no ops");
            return;
        }

        ensureTrue(ids.size() == embeddings.size(), "ids size is not equal to embeddings size");
        ensureTrue(embedded == null || embeddings.size() == embedded.size(),
                "embeddings size is not equal to embedded size");

        String tableName = this.schema + "." + this.table;

        boolean hasBizData = false;
        if (bizDataHandler != null && StringUtil.hasText(bizDataHandler.columnDefinitionsString())) {
            hasBizData = true;
        }

        try (Connection connection = getConnection()) {
            String query;
            if (hasBizData) {
                query = String.format(
                        "INSERT INTO %s (id, embedding, doc_chunk, %s, %s) VALUES (?, ?, ?, %s, %s)" +
                                "ON CONFLICT (id) DO UPDATE SET " +
                                "embedding = EXCLUDED.embedding," +
                                "doc_chunk = EXCLUDED.doc_chunk," +
                                "%s," +
                                "%s;",
                        tableName,
                        join(",", bizDataHandler.columnsNames()),
                        join(",", metadataHandler.columnsNames()),
                        join(",", nCopies(bizDataHandler.columnsNames().size(), "?")),
                        join(",", nCopies(metadataHandler.columnsNames().size(), "?")),
                        bizDataHandler.insertClause(),
                        metadataHandler.insertClause());
            } else {
                query = String.format(
                        "INSERT INTO %s (id, embedding, doc_chunk, %s) VALUES (?, ?, ?, %s)" +
                                "ON CONFLICT (id) DO UPDATE SET " +
                                "embedding = EXCLUDED.embedding," +
                                "doc_chunk = EXCLUDED.doc_chunk," +
                                "%s;",
                        tableName, join(",", metadataHandler.columnsNames()),
                        join(",", nCopies(metadataHandler.columnsNames().size(), "?")),
                        metadataHandler.insertClause());
            }

            try (PreparedStatement upsertStmt = connection.prepareStatement(query)) {
                for (int i = 0; i < ids.size(); ++i) {
                    upsertStmt.setObject(1, ids.get(i));
                    upsertStmt.setObject(2, new PGvector(embeddings.get(i).embedding()));

                    if (embedded != null && embedded.get(i) != null) {
                        upsertStmt.setObject(3, embedded.get(i).text());
                        if (hasBizData) {
                            bizDataHandler.setBizData(upsertStmt, 4, embedded.get(i).bizData());
                            metadataHandler.setMetadata(upsertStmt, 5, embedded.get(i).metadata());
                        } else {
                            metadataHandler.setMetadata(upsertStmt, 4, embedded.get(i).metadata());
                        }
                    } else {
                        upsertStmt.setNull(3, Types.VARCHAR);
                        if (hasBizData) {
                            IntStream.range(4, 4 + bizDataHandler.columnsNames().size()).forEach(
                                    j -> {
                                        try {
                                            upsertStmt.setNull(j, Types.OTHER);
                                        } catch (SQLException e) {
                                            throw new RuntimeException(e);
                                        }
                                    });
                            IntStream.range(5 + bizDataHandler.columnsNames().size(),
                                    5 + bizDataHandler.columnsNames().size() + metadataHandler.columnsNames().size()).forEach(
                                    j -> {
                                        try {
                                            upsertStmt.setNull(j, Types.OTHER);
                                        } catch (SQLException e) {
                                            throw new RuntimeException(e);
                                        }
                                    });
                        } else {
                            IntStream.range(4, 4 + metadataHandler.columnsNames().size()).forEach(
                                    j -> {
                                        try {
                                            upsertStmt.setNull(j, Types.OTHER);
                                        } catch (SQLException e) {
                                            throw new RuntimeException(e);
                                        }
                                    });
                        }

                    }
                    upsertStmt.addBatch();
                }
                upsertStmt.executeBatch();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private PgVectorConfig config;
        private String table;
        private PgIndexType indexType = PgIndexType.IVFFLAT;
        private PgDistanceType distanceType = PgDistanceType.COSINE_DISTANCE;
        private MetadataStorageConfig metadataStorageConfig = DefaultMetadataStorageConfig.defaultConfig();

        private BizDataHandler bizDataHandler;

        public Builder config(PgVectorConfig config) {
            this.config = config;
            return this;
        }
        public Builder table(String table) {
            this.table = table;
            return this;
        }
        public Builder PgIndexType(PgIndexType indexType) {
            this.indexType = indexType;
            return this;
        }
        public Builder PgIndexType(PgDistanceType distanceType) {
            this.distanceType = distanceType;
            return this;
        }
        public Builder metadataStorageConfig(MetadataStorageConfig metadataStorageConfig) {
            this.metadataStorageConfig = metadataStorageConfig;
            return this;
        }

        public Builder bizDataHandler(BizDataHandler bizDataHandler) {
            this.bizDataHandler = bizDataHandler;
            return this;
        }

        public PgVectorStore build() {
            return new PgVectorStore(config, table, indexType, distanceType, metadataStorageConfig, bizDataHandler);
        }
    }
}
