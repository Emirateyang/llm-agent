package com.llmagent.vector.store;

import com.llmagent.Experimental;
import com.llmagent.vector.store.filter.Filter;

import java.util.Collection;
import java.util.List;

import static java.util.Collections.singletonList;

public interface EmbeddingStore<T> {
    /**
     * Adds a given embedding to the store.
     *
     * @param embedding The embedding to be added to the store.
     * @return The auto-generated ID associated with the added embedding.
     */
    String add(VectorData embedding);

    /**
     * Adds a given embedding to the store.
     *
     * @param id        The unique identifier for the embedding to be added.
     * @param embedding The embedding to be added to the store.
     */
    void add(String id, VectorData embedding);

    /**
     * Adds a given embedding and the corresponding content that has been T to the store.
     *
     * @param embedding The embedding to be added to the store.
     * @param T  Original content that was T.
     * @return The auto-generated ID associated with the added embedding.
     */
    String add(VectorData embedding, T T);

    /**
     * Adds multiple embeddings to the store.
     *
     * @param embeddings A list of embeddings to be added to the store.
     * @return A list of auto-generated IDs associated with the added embeddings.
     */
    List<String> addAll(List<VectorData> embeddings);

    /**
     * Adds multiple embeddings and their corresponding contents that have been T to the store.
     *
     * @param embeddings A list of embeddings to be added to the store.
     * @param T   A list of original contents that were T.
     * @return A list of auto-generated IDs associated with the added embeddings.
     */
    List<String> addAll(List<VectorData> embeddings, List<T> T);

    /**
     * Adds multiple embeddings and their corresponding contents that have been embedded to the store.
     *
     * @param ids        A list of IDs associated with the added embeddings.
     * @param embeddings A list of embeddings to be added to the store.
     * @param T   A list of original contents that were T.
     */
    default void addAll(List<String> ids, List<VectorData> embeddings, List<T> T) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Removes a single embedding from the store by ID.
     *
     * @param id The unique ID of the embedding to be removed.
     */
    @Experimental
    default void remove(String id) {
        this.removeAll(singletonList(id));
    }

    /**
     * Removes all embeddings that match the specified IDs from the store.
     *
     * @param ids A collection of unique IDs of the embeddings to be removed.
     */
    @Experimental
    default void removeAll(Collection<String> ids) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Removes all embeddings that match the specified {@link Filter} from the store.
     *
     * @param filter The filter to be applied to the {@link Metadata} of the {@link TextSegment} during removal.
     *               Only embeddings whose {@code TextSegment}'s {@code Metadata}
     *               match the {@code Filter} will be removed.
     */
    @Experimental
    default void removeAll(Filter filter) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Removes all embeddings from the store.
     */
    @Experimental
    default void removeAll() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Searches for the most similar (closest in the embedding space) {@link VectorData}s.
     * <br>
     * All search criteria are defined inside the {@link EmbeddingSearchRequest}.
     * <br>
     * {@link EmbeddingSearchRequest#filter()} can be used to filter by user/memory ID.
     * Please note that not all {@link EmbeddingStore} implementations support {@link Filter}ing.
     *
     * @param request A request to search in an {@link EmbeddingStore}. Contains all search criteria.
     * @return An {@link EmbeddingSearchResult} containing all found {@link VectorData}s.
     */
    default EmbeddingSearchResult<T> search(EmbeddingSearchRequest request) {
        List<EmbeddingMatch<T>> matches =
                findRelevant(request.queryEmbedding(), request.maxResults(), request.minScore());
        return new EmbeddingSearchResult<>(matches);
    }


    /**
     * Finds the most relevant (closest in space) embeddings to the provided reference embedding.
     *
     * @param referenceEmbedding The embedding used as a reference. Returned embeddings should be relevant (closest) to this one.
     * @param maxResults         The maximum number of embeddings to be returned.
     * @param minScore           The minimum relevance score, ranging from 0 to 1 (inclusive).
     *                           Only embeddings with a score of this value or higher will be returned.
     * @return A list of embedding matches.
     * Each embedding match includes a relevance score (derivative of cosine distance),
     * ranging from 0 (not relevant) to 1 (highly relevant).
     * @deprecated as of 0.31.0, use {@link #search(EmbeddingSearchRequest)} instead.
     */
    @Deprecated
    default List<EmbeddingMatch<T>> findRelevant(VectorData referenceEmbedding, int maxResults, double minScore) {
        EmbeddingSearchRequest embeddingSearchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(referenceEmbedding)
                .maxResults(maxResults)
                .minScore(minScore)
                .build();
        EmbeddingSearchResult<T> embeddingSearchResult = search(embeddingSearchRequest);
        return embeddingSearchResult.matches();
    }

    /**
     * Finds the most relevant (closest in space) embeddings to the provided reference embedding.
     * By default, minScore is set to 0, which means that the results may include embeddings with low relevance.
     *
     * @param memoryId           The memoryId used Distinguishing query requests from different users.
     * @param referenceEmbedding The embedding used as a reference. Returned embeddings should be relevant (closest) to this one.
     * @param maxResults         The maximum number of embeddings to be returned.
     * @return A list of embedding matches.
     * Each embedding match includes a relevance score (derivative of cosine distance),
     * ranging from 0 (not relevant) to 1 (highly relevant).
     * @deprecated as of 0.31.0, use {@link #search(EmbeddingSearchRequest)} instead.
     */
    @Deprecated
    default List<EmbeddingMatch<T>> findRelevant(
            Object memoryId, VectorData referenceEmbedding, int maxResults) {
        return findRelevant(memoryId, referenceEmbedding, maxResults, 0);
    }

    /**
     * Finds the most relevant (closest in space) embeddings to the provided reference embedding.
     *
     * @param memoryId           The memoryId used Distinguishing query requests from different users.
     * @param referenceEmbedding The embedding used as a reference. Returned embeddings should be relevant (closest) to this one.
     * @param maxResults         The maximum number of embeddings to be returned.
     * @param minScore           The minimum relevance score, ranging from 0 to 1 (inclusive).
     *                           Only embeddings with a score of this value or higher will be returned.
     * @return A list of embedding matches.
     * Each embedding match includes a relevance score (derivative of cosine distance),
     * ranging from 0 (not relevant) to 1 (highly relevant).
     * @deprecated as of 0.31.0, use {@link #search(EmbeddingSearchRequest)} instead.
     */
    @Deprecated
    default List<EmbeddingMatch<T>> findRelevant(
            Object memoryId, VectorData referenceEmbedding, int maxResults, double minScore) {
        throw new RuntimeException("Not implemented");
    }
}
