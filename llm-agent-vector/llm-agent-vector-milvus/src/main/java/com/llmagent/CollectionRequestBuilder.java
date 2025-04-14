package com.llmagent;

import com.llmagent.vector.store.filter.Filter;
import io.milvus.common.clientenum.ConsistencyLevelEnum;
import io.milvus.param.MetricType;
import io.milvus.param.collection.DropCollectionParam;
import io.milvus.param.collection.FlushParam;
import io.milvus.param.collection.HasCollectionParam;
import io.milvus.param.collection.LoadCollectionParam;
import io.milvus.param.dml.DeleteParam;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.QueryParam;
import io.milvus.param.dml.SearchParam;

import java.util.List;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;

/**
 * The CollectionRequestBuilder class provides static methods to construct various types of request parameters
 * for interacting with a collection in Milvus. These methods use the builder pattern to create immutable objects step-by-step,
 * which is a common design pattern in Java for creating complex objects in a readable and maintainable way.
 * <br>
 * The builder pattern allows for the creation of these objects in a fluent and readable manner, making the
 * code easier to understand and maintain.
 */
public class CollectionRequestBuilder {

    static FlushParam buildFlushRequest(String collectionName) {
        return FlushParam.newBuilder()
                .withCollectionNames(singletonList(collectionName))
                .build();
    }

    static HasCollectionParam buildHasCollectionRequest(String collectionName) {
        return HasCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .build();
    }

    static DropCollectionParam buildDropCollectionRequest(String collectionName) {
        return DropCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .build();
    }

    //This method builds an InsertParam object with the given collection name and fields
    static InsertParam buildInsertRequest(String collectionName, List<InsertParam.Field> fields) {
        //Create a new InsertParam object
        return InsertParam.newBuilder()
                //Set the collection name
                .withCollectionName(collectionName)
                //Set the fields
                .withFields(fields)
                //Build the object
                .build();
    }

    static LoadCollectionParam buildLoadCollectionInMemoryRequest(String collectionName) {
        return LoadCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .build();
    }

    static SearchParam buildSearchRequest(String collectionName,
                                          FieldDefinition fieldDefinition,
                                          List<Float> vector,
                                          Filter filter,
                                          int maxResults,
                                          MetricType metricType,
                                          ConsistencyLevelEnum consistencyLevel) {
        SearchParam.Builder builder = SearchParam.newBuilder()
                .withCollectionName(collectionName)
                .withVectors(singletonList(vector))
                .withVectorFieldName(fieldDefinition.getVectorFieldName())
                .withTopK(maxResults)
                .withMetricType(metricType)
                .withConsistencyLevel(consistencyLevel)
                .withOutFields(asList(fieldDefinition.getIdFieldName(), fieldDefinition.getTextFieldName(), fieldDefinition.getMetadataFieldName()));

        if (filter != null) {
            builder.withExpr(MilvusMetadataFilterMapper.map(filter, fieldDefinition.getMetadataFieldName()));
        }

        return builder.build();
    }

    static QueryParam buildQueryRequest(String collectionName,
                                        FieldDefinition fieldDefinition,
                                        List<String> rowIds,
                                        ConsistencyLevelEnum consistencyLevel) {
        return QueryParam.newBuilder()
                .withCollectionName(collectionName)
                .withExpr(buildQueryExpression(rowIds, fieldDefinition.getIdFieldName()))
                .withConsistencyLevel(consistencyLevel)
                .withOutFields(singletonList(fieldDefinition.getVectorFieldName()))
                .build();
    }

    static DeleteParam buildDeleteRequest(String collectionName,
                                          String expr) {
        return DeleteParam.newBuilder()
                .withCollectionName(collectionName)
                .withExpr(expr)
                .build();
    }

    private static String buildQueryExpression(List<String> rowIds, String idFieldName) {
        return rowIds.stream()
                .map(id -> format("%s == '%s'", idFieldName, id))
                .collect(joining(" || "));
    }
}
