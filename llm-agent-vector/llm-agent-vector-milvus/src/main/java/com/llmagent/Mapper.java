package com.llmagent;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.ToNumberPolicy;
import com.google.gson.reflect.TypeToken;
import com.llmagent.data.Metadata;
import com.llmagent.data.segment.TextSegment;
import com.llmagent.vector.store.EmbeddingMatch;
import com.llmagent.vector.store.RelevanceScore;
import com.llmagent.vector.store.VectorData;
import io.milvus.client.MilvusServiceClient;
import io.milvus.common.clientenum.ConsistencyLevelEnum;
import io.milvus.exception.ParamException;
import io.milvus.response.QueryResultsWrapper;
import io.milvus.response.SearchResultsWrapper;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.llmagent.CollectionOperationsExecutor.queryForVectors;
import static com.llmagent.Generator.generateEmptyJsons;
import static com.llmagent.Generator.generateEmptyScalars;
import static com.llmagent.util.ObjectUtil.isNullOrEmpty;
import static com.llmagent.util.StringUtil.isNullOrBlank;
import static java.util.stream.Collectors.toList;

public class Mapper {
    private static final Gson GSON = new GsonBuilder()
            .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
            .create();

    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() {
    }.getType();

    static List<List<Float>> toVectors(List<VectorData> embeddings) {
        return embeddings.stream()
                .map(VectorData::vectorAsList)
                .collect(toList());
    }

    static List<String> toScalars(List<TextSegment> textSegments, int size) {
        return isNullOrEmpty(textSegments) ? generateEmptyScalars(size) : textSegmentsToScalars(textSegments);
    }

    static List<JsonObject> toMetadataJsons(List<TextSegment> textSegments, int size) {
        return isNullOrEmpty(textSegments) ? generateEmptyJsons(size) : textSegments.stream()
                .map(segment -> GSON.toJsonTree(segment.metadata().toMap()).getAsJsonObject())
                .collect(toList());
    }

    static List<String> textSegmentsToScalars(List<TextSegment> textSegments) {
        return textSegments.stream()
                .map(TextSegment::text)
                .collect(toList());
    }

    static List<EmbeddingMatch<TextSegment>> toEmbeddingMatches(MilvusServiceClient milvusClient,
                                                                SearchResultsWrapper resultsWrapper,
                                                                String collectionName,
                                                                FieldDefinition fieldDefinition,
                                                                ConsistencyLevelEnum consistencyLevel,
                                                                boolean queryForVectorOnSearch) {
        List<EmbeddingMatch<TextSegment>> matches = new ArrayList<>();

        Map<String, VectorData> idToEmbedding = new HashMap<>();
        if (queryForVectorOnSearch) {
            try {
                List<String> rowIds = (List<String>) resultsWrapper.getFieldWrapper(fieldDefinition.getIdFieldName()).getFieldData();
                idToEmbedding.putAll(queryEmbeddings(milvusClient, collectionName, fieldDefinition, rowIds, consistencyLevel));
            } catch (ParamException e) {
                // There is no way to check if the result is empty or not.
                // If the result is empty, the exception will be thrown.
            }
        }

        for (int i = 0; i < resultsWrapper.getRowRecords().size(); i++) {
            double score = resultsWrapper.getIDScore(0).get(i).getScore();
            String rowId = resultsWrapper.getIDScore(0).get(i).getStrID();
            VectorData embedding = idToEmbedding.get(rowId);
            TextSegment textSegment = toTextSegment(resultsWrapper.getRowRecords().get(i), fieldDefinition);
            EmbeddingMatch<TextSegment> embeddingMatch = new EmbeddingMatch<>(
                    RelevanceScore.fromCosineSimilarity(score),
                    rowId,
                    embedding,
                    textSegment
            );
            matches.add(embeddingMatch);
        }

        return matches;
    }

    private static TextSegment toTextSegment(QueryResultsWrapper.RowRecord rowRecord, FieldDefinition fieldDefinition) {

        Object textField = rowRecord.get(fieldDefinition.getTextFieldName());
        String text = textField == null ? null : textField.toString();
        if (isNullOrBlank(text)) {
            return null;
        }

        if (!rowRecord.getFieldValues().containsKey(fieldDefinition.getMetadataFieldName())) {
            return TextSegment.from(text);
        }

        JsonObject metadata = (JsonObject) rowRecord.get(fieldDefinition.getMetadataFieldName());
        return TextSegment.from(text, toMetadata(metadata));
    }

    private static Metadata toMetadata(JsonObject metadata) {
        Map<String, Object> metadataMap = GSON.fromJson(metadata, MAP_TYPE);
        metadataMap.forEach((key, value) -> {
            if (value instanceof BigDecimal) {
                // It is safe to convert. No information is lost, the "biggest" type allowed in Metadata is double.
                metadataMap.put(key, ((BigDecimal) value).doubleValue());
            }
        });
        return Metadata.from(metadataMap);
    }

    private static Map<String, VectorData> queryEmbeddings(MilvusServiceClient milvusClient,
                                                          String collectionName,
                                                          FieldDefinition fieldDefinition,
                                                          List<String> rowIds,
                                                          ConsistencyLevelEnum consistencyLevel) {
        QueryResultsWrapper queryResultsWrapper = queryForVectors(
                milvusClient,
                collectionName,
                fieldDefinition,
                rowIds,
                consistencyLevel
        );

        Map<String, VectorData> idToEmbedding = new HashMap<>();
        for (QueryResultsWrapper.RowRecord row : queryResultsWrapper.getRowRecords()) {
            String id = row.get(fieldDefinition.getIdFieldName()).toString();
            List<Float> vector = (List<Float>) row.get(fieldDefinition.getVectorFieldName());
            idToEmbedding.put(id, VectorData.from(vector));
        }

        return idToEmbedding;
    }
}
