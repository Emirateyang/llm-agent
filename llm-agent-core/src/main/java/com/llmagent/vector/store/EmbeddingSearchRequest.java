package com.llmagent.vector.store;

import com.llmagent.data.Metadata;
import com.llmagent.data.segment.TextSegment;
import com.llmagent.vector.store.filter.Filter;
import lombok.Builder;
import lombok.EqualsAndHashCode;

import static com.llmagent.util.ObjectUtil.getOrDefault;

@EqualsAndHashCode
public class EmbeddingSearchRequest {
    private final VectorData queryEmbedding;
    private final int maxResults;
    private final double minScore;
    private final Filter filter;

    /**
     * Creates an instance of an EmbeddingSearchRequest.
     *
     * @param queryEmbedding The embedding used as a reference. Found embeddings should be similar to this one.
     *                       This is a mandatory parameter.
     * @param maxResults     The maximum number of embeddings to return. This is an optional parameter. Default: 3
     * @param minScore       The minimum score, ranging from 0 to 1 (inclusive).
     *                       Only embeddings with a score &gt;= minScore will be returned.
     *                       This is an optional parameter. Default: 0
     * @param filter         The filter to be applied to the {@link Metadata} during search.
     *                       Only {@link TextSegment}s whose {@link Metadata}
     *                       matches the {@link Filter} will be returned.
     *                       Please note that not all {@link EmbeddingStore}s support this feature yet.
     *                       This is an optional parameter. Default: no filtering
     */
    @Builder
    public EmbeddingSearchRequest(VectorData queryEmbedding, Integer maxResults, Double minScore, Filter filter) {
        this.queryEmbedding = queryEmbedding;
        this.maxResults = getOrDefault(maxResults, 3);
        this.minScore = getOrDefault(minScore, 0.0);
        this.filter = filter;
    }

    public VectorData queryEmbedding() {
        return queryEmbedding;
    }

    public int maxResults() {
        return maxResults;
    }

    public double minScore() {
        return minScore;
    }

    public Filter filter() {
        return filter;
    }
}
