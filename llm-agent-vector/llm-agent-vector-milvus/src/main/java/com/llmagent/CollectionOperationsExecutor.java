package com.llmagent;

import com.llmagent.exception.MilvusFailedException;
import io.milvus.client.MilvusServiceClient;
import io.milvus.common.clientenum.ConsistencyLevelEnum;
import io.milvus.grpc.FlushResponse;
import io.milvus.grpc.MutationResult;
import io.milvus.grpc.QueryResults;
import io.milvus.grpc.SearchResults;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.RpcStatus;
import io.milvus.param.collection.*;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.QueryParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.response.QueryResultsWrapper;
import io.milvus.response.SearchResultsWrapper;

import java.util.List;

import static com.llmagent.CollectionRequestBuilder.*;
import static io.milvus.grpc.DataType.*;
import static java.lang.String.format;

/**
 * The CollectionOperationsExecutor class provides static methods to perform various operations on a collection
 * in Milvus. Milvus is used for similarity search and vector analytics. This class encapsulates
 * common operations such as creating, dropping, and managing collections, as well as performing searches and queries.
 *
 */
public class CollectionOperationsExecutor {
    // Flushes the collection in Milvus DB
    static void flush(MilvusServiceClient milvusClient, String collectionName) {
        // Build the flush request
        FlushParam request = buildFlushRequest(collectionName);
        // Send the flush request to Milvus DB
        R<FlushResponse> response = milvusClient.flush(request);
        // Check if the response is successful
        checkResponseNotFailed(response);
    }

    // Checks if the collection exists in Milvus DB
    static boolean hasCollection(MilvusServiceClient milvusClient, String collectionName) {
        // Build the has collection request
        HasCollectionParam request = buildHasCollectionRequest(collectionName);
        // Send the has collection request to Milvus DB
        R<Boolean> response = milvusClient.hasCollection(request);
        // Check if the response is successful
        checkResponseNotFailed(response);
        // Return the response data
        return response.getData();
    }

    // Creates a collection in Milvus DB
    static void createCollection(MilvusServiceClient milvusClient, String collectionName, FieldDefinition fieldDefinition, int dimension) {

        // Build the create collection request
        CreateCollectionParam request = CreateCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .withSchema(CollectionSchemaParam.newBuilder()
                        .addFieldType(FieldType.newBuilder()
                                .withName(fieldDefinition.getIdFieldName())
                                .withDataType(VarChar)
                                .withMaxLength(36)
                                .withPrimaryKey(true)
                                .withAutoID(false)
                                .build())
                        .addFieldType(FieldType.newBuilder()
                                .withName(fieldDefinition.getTextFieldName())
                                .withDataType(VarChar)
                                .withMaxLength(65535)
                                .build())
                        .addFieldType(FieldType.newBuilder()
                                .withName(fieldDefinition.getMetadataFieldName())
                                .withDataType(JSON)
                                .build())
                        .addFieldType(FieldType.newBuilder()
                                .withName(fieldDefinition.getVectorFieldName())
                                .withDataType(FloatVector)
                                .withDimension(dimension)
                                .build())
                        .build()
                )
                .build();

        // Send the create collection request to Milvus DB
        R<RpcStatus> response = milvusClient.createCollection(request);
        // Check if the response is successful
        checkResponseNotFailed(response);
    }

    // Drops a collection from Milvus DB
    static void dropCollection(MilvusServiceClient milvusClient, String collectionName) {
        // Build the drop collection request
        DropCollectionParam request = buildDropCollectionRequest(collectionName);
        // Send the drop collection request to Milvus DB
        R<RpcStatus> response = milvusClient.dropCollection(request);
        // Check if the response is successful
        checkResponseNotFailed(response);
    }

    // Creates an index in Milvus DB
    static void createIndex(MilvusServiceClient milvusClient,
                            String collectionName,
                            String vectorFieldName,
                            IndexType indexType,
                            MetricType metricType) {

        // Build the create index request
        CreateIndexParam request = CreateIndexParam.newBuilder()
                .withCollectionName(collectionName)
                .withFieldName(vectorFieldName)
                .withIndexType(indexType)
                .withMetricType(metricType)
                .build();

        // Send the create index request to Milvus DB
        R<RpcStatus> response = milvusClient.createIndex(request);
        // Check if the response is successful
        checkResponseNotFailed(response);
    }

    // Inserts data into Milvus DB
    static void insert(MilvusServiceClient milvusClient, String collectionName, List<InsertParam.Field> fields) {
        // Build the insert request
        InsertParam request = buildInsertRequest(collectionName, fields);
        // Send the insert request to Milvus DB
        R<MutationResult> response = milvusClient.insert(request);
        // Check if the response is successful
        checkResponseNotFailed(response);
    }

    // Loads a collection into memory in Milvus DB
    static void loadCollectionInMemory(MilvusServiceClient milvusClient, String collectionName) {
        // Build the load collection in memory request
        LoadCollectionParam request = buildLoadCollectionInMemoryRequest(collectionName);
        // Send the load collection in memory request to Milvus DB
        R<RpcStatus> response = milvusClient.loadCollection(request);
        // Check if the response is successful
        checkResponseNotFailed(response);
    }

    // Searches for data in Milvus DB
    static SearchResultsWrapper search(MilvusServiceClient milvusClient, SearchParam searchRequest) {
        // Send the search request to Milvus DB
        R<SearchResults> response = milvusClient.search(searchRequest);
        // Check if the response is successful
        checkResponseNotFailed(response);

        // Return the search results
        return new SearchResultsWrapper(response.getData().getResults());
    }

    // Queries for vectors in Milvus DB
    static QueryResultsWrapper queryForVectors(MilvusServiceClient milvusClient,
                                               String collectionName,
                                               FieldDefinition fieldDefinition,
                                               List<String> rowIds,
                                               ConsistencyLevelEnum consistencyLevel) {
        // Build the query request
        QueryParam request = buildQueryRequest(collectionName, fieldDefinition, rowIds, consistencyLevel);
        // Send the query request to Milvus DB
        R<QueryResults> response = milvusClient.query(request);
        // Check if the response is successful
        checkResponseNotFailed(response);

        // Return the query results
        return new QueryResultsWrapper(response.getData());
    }

    // Removes vectors from Milvus DB
    static void removeForVector(MilvusServiceClient milvusClient,
                                String collectionName,
                                String expr) {
        // Send the delete request to Milvus DB
        R<MutationResult> response = milvusClient.delete(buildDeleteRequest(collectionName, expr));
        // Check if the response is successful
        checkResponseNotFailed(response);
    }

    // Checks if the response is successful
    private static <T> void checkResponseNotFailed(R<T> response) {
        if (response == null) {
            throw new MilvusFailedException("Request to Milvus DB failed. Response is null");
        } else if (response.getStatus() != R.Status.Success.getCode()) {
            String message = format("Request to Milvus DB failed. Response status:'%d'.%n", response.getStatus());
            throw new MilvusFailedException(message, response.getException());
        }
    }
}
