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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.llmagent.document.Document;
import com.llmagent.util.Maps;
import com.llmagent.util.StringUtil;
import com.llmagent.util.VectorUtil;
import com.llmagent.vector.store.DocumentStore;
import com.llmagent.vector.store.SearchWrapper;
import com.llmagent.vector.store.StoreOptions;
import com.llmagent.vector.store.StoreResult;
import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.ConsistencyLevel;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.exception.MilvusClientException;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.GetLoadStateReq;
import io.milvus.v2.service.vector.request.DeleteReq;
import io.milvus.v2.service.vector.request.InsertReq;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.UpsertReq;
import io.milvus.v2.service.vector.response.InsertResp;
import io.milvus.v2.service.vector.response.SearchResp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class MilvusVectorStore extends DocumentStore {

    private static final Logger logger = LoggerFactory.getLogger(MilvusVectorStore.class);
    private final MilvusClientV2 client;
    private final String defaultCollectionName;
    private final MilvusConfig config;

    public MilvusVectorStore(MilvusConfig config) {
        ConnectConfig connectConfig = ConnectConfig.builder()
                .uri(config.getUri())
                .dbName(config.getDatabaseName())
                .token(config.getToken())
                .username(config.getUsername())
                .password(config.getPassword())
                .build();

        this.client = new MilvusClientV2(connectConfig);
        this.defaultCollectionName = config.getDefaultCollectionName();
        this.config = config;
    }

    @Override
    public StoreResult storeImplement(List<Document> documents, StoreOptions options) {
        List<JSONObject> data = new ArrayList<>();
        for (Document doc : documents) {
            JSONObject dict = new JSONObject();
            dict.put("id", String.valueOf(doc.getId()));
            dict.put("content", doc.getContent());
            dict.put("vector", VectorUtil.toFloatList(doc.getVector()));

            Map<String, Object> metaDataMap = doc.getMetadataMap();
            JSONObject jsonObject = JSON.parseObject(JSON.toJSONBytes(metaDataMap == null ? Collections.EMPTY_MAP : metaDataMap));
            dict.put("metadata", jsonObject);
            data.add(dict);
        }

        String collectionName = options.getCollectionNameOrDefault(defaultCollectionName);
        InsertReq.InsertReqBuilder<?, ?> builder = InsertReq.builder();
        if (StringUtil.hasText(options.getPartitionName())) {
            builder.partitionName(options.getPartitionName());
        }
        InsertReq insertReq = builder
                .collectionName(collectionName)
                .data(data)
                .build();
        try {
            InsertResp insertResp = client.insert(insertReq);
        } catch (MilvusClientException e) {
            if (e.getMessage() != null && e.getMessage().contains("collection not found")
                    && config.isAutoCreateCollection()
                    && options.getMetaData("forInternal") == null) {

                Boolean success = createCollection(collectionName);
                if (success != null && success) {
                    //store
                    options.addMetaData("forInternal", true);
                    storeImplement(documents, options);
                }
            } else {
                return StoreResult.fail();
            }
        }

        return StoreResult.successWithIds(documents);
    }

    /**
     * Create collection if not exist
     * @param collectionName collection name to create
     * @return true if create collection success, false if create collection failed
     */
    private Boolean createCollection(String collectionName) {
        List<CreateCollectionReq.FieldSchema> fieldSchemaList = new ArrayList<>();

        //id
        CreateCollectionReq.FieldSchema id = CreateCollectionReq.FieldSchema.builder()
                .name("id")
                .dataType(DataType.VarChar)
                .maxLength(36)
                .isPrimaryKey(true)
                .autoID(false)
                .build();
        fieldSchemaList.add(id);

        //content
        CreateCollectionReq.FieldSchema content = CreateCollectionReq.FieldSchema.builder()
                .name("content")
                .dataType(DataType.VarChar)
                .maxLength(65535)
                .build();
        fieldSchemaList.add(content);

        //metadata
        CreateCollectionReq.FieldSchema metadata = CreateCollectionReq.FieldSchema.builder()
                .name("metadata")
                .dataType(DataType.JSON)
                .build();
        fieldSchemaList.add(metadata);

        //vector
        CreateCollectionReq.FieldSchema vector = CreateCollectionReq.FieldSchema.builder()
                .name("vector")
                .dataType(DataType.FloatVector)
                .dimension(this.getEmbeddingModel().dimensions())
                .build();
        fieldSchemaList.add(vector);

        CreateCollectionReq.CollectionSchema collectionSchema = CreateCollectionReq.CollectionSchema
                .builder()
                .fieldSchemaList(fieldSchemaList)
                .build();

        List<IndexParam> indexParams = new ArrayList<>();
        IndexParam vectorIndex = IndexParam.builder().fieldName("vector")
                .indexType(IndexParam.IndexType.IVF_FLAT)
                .metricType(IndexParam.MetricType.COSINE)
                .indexName("vector")
                .extraParams(Maps.of("nlist", 1024).build())
                .build();
        indexParams.add(vectorIndex);

        CreateCollectionReq createCollectionReq = CreateCollectionReq.builder()
                .collectionName(collectionName)
                .collectionSchema(collectionSchema)
                .primaryFieldName("id")
                .vectorFieldName("vector")
                .description("Llm Agent Vector Store")
                .indexParams(indexParams)
                .build();

        client.createCollection(createCollectionReq);

        GetLoadStateReq quickSetupLoadStateReq = GetLoadStateReq.builder()
                .collectionName(collectionName)
                .build();

        return client.getLoadState(quickSetupLoadStateReq);
    }

    @Override
    public StoreResult deleteImplement(Collection<Object> ids, StoreOptions options) {

        DeleteReq.DeleteReqBuilder<?, ?> builder = DeleteReq.builder();
        if (StringUtil.hasText(options.getPartitionName())) {
            builder.partitionName(options.getPartitionName());
        }

        DeleteReq deleteReq = builder
                .collectionName(options.getCollectionNameOrDefault(defaultCollectionName))
                .ids(new ArrayList<>(ids))
                .build();

        try {
            client.delete(deleteReq);
        } catch (Exception e) {
            logger.error("delete document error: " + e.getMessage(), e);
            return StoreResult.fail();
        }

        return StoreResult.success();

    }

    /**
     * update document
     * @param documents list of documents to update
     * @param options store options
     * @return store result
     */
    @Override
    public StoreResult updateImplement(List<Document> documents, StoreOptions options) {
        if (documents == null || documents.isEmpty()) {
            return StoreResult.success();
        }
        List<JSONObject> data = new ArrayList<>();
        for (Document doc : documents) {
            JSONObject dict = new JSONObject();

            dict.put("id", doc.getId());
            dict.put("content", doc.getContent());
            dict.put("vector", VectorUtil.toFloatList(doc.getVector()));

            Map<String, Object> metadatas = doc.getMetadataMap();
            JSONObject jsonObject = JSON.parseObject(JSON.toJSONBytes(metadatas == null ? Collections.EMPTY_MAP : metadatas));
            dict.put("metadata", jsonObject);
            data.add(dict);

            data.add(dict);
        }

        UpsertReq upsertReq = UpsertReq.builder()
                .collectionName(options.getCollectionNameOrDefault(defaultCollectionName))
                .partitionName(options.getPartitionName())
                .data(data)
                .build();
        client.upsert(upsertReq);
        return StoreResult.successWithIds(documents);
    }

    @Override
    public List<Document> searchImplement(SearchWrapper searchWrapper, StoreOptions options) {
        List<String> outputFields = searchWrapper.isOutputVector()
                ? Arrays.asList("id", "vector", "content", "metadata")
                : Arrays.asList("id", "content", "metadata");

        SearchReq.SearchReqBuilder<?, ?> builder = SearchReq.builder();
        if (StringUtil.hasText(options.getPartitionName())) {
            builder.partitionNames(options.getPartitionNamesOrEmpty());
        }

        SearchReq searchReq = builder
                .collectionName(options.getCollectionNameOrDefault(defaultCollectionName))
                .consistencyLevel(ConsistencyLevel.STRONG)
                .outputFields(outputFields)
                .topK(searchWrapper.getMaxResults())
                .annsField("vector")
                .data(Collections.singletonList(VectorUtil.toFloatList(searchWrapper.getVector())))
                .filter(searchWrapper.toFilterExpression(MilvusExpressionAdaptor.DEFAULT))
                .build();

        try {
            SearchResp resp = client.search(searchReq);
            // Parse and convert search results to Document list
            List<List<SearchResp.SearchResult>> results = resp.getSearchResults();
            List<Document> documents = new ArrayList<>();
            for (List<SearchResp.SearchResult> resultList : results) {
                for (SearchResp.SearchResult result : resultList) {
                    Map<String, Object> entity = result.getEntity();
                    if (entity == null || entity.isEmpty()) {
                        continue;
                    }

                    Document doc = new Document();
                    doc.setId(result.getId());

                    Object vectorObj = entity.get("vector");
                    if (vectorObj instanceof List) {
                        //noinspection unchecked
                        doc.setVector(VectorUtil.convertToVector((List<Float>) vectorObj));
                    }

                    doc.setContent((String) entity.get("content"));

                    JSONObject object = (JSONObject) entity.get("metadata");
                    doc.addMetadata(object);

                    doc.addMetadata(entity);
                    documents.add(doc);
                }
            }
            return documents;
        } catch (Exception e) {
            logger.error("Error searching in Milvus", e);
            return Collections.emptyList();
        }
    }

    public MilvusClientV2 getClient() {
        return client;
    }
}
