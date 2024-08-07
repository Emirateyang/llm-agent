package com.llmagent.vector.store;

import java.util.Objects;

public class EmbeddingMatch<T> {

    private final Double score;
    private final String embeddingId;
    private final VectorData embedding;
    private final T embedded;

    /**
     * Creates a new instance.
     * @param score The relevance score (derivative of cosine distance) of this embedding compared to
     *              a reference embedding during a search.
     * @param embeddingId The ID of the embedding assigned when adding this embedding to the store.
     * @param embedding The embedding that has been matched.
     * @param embedded The original content that was embedded. Typically, this is a {@link com.llmagent.data.segment.TextSegment}.
     */
    public EmbeddingMatch(Double score, String embeddingId, VectorData embedding, T embedded) {
        this.score = score;
        this.embeddingId = embeddingId;
        this.embedding = embedding;
        this.embedded = embedded;
    }

    /**
     * Returns the relevance score (derivative of cosine distance) of this embedding compared to
     * a reference embedding during a search.
     * The current implementation assumes that the embedding store uses cosine distance when comparing embeddings.
     *
     * @return Relevance score, ranging from 0 (not relevant) to 1 (highly relevant).
     */
    public Double score() {
        return score;
    }

    /**
     * The ID of the embedding assigned when adding this embedding to the store.
     * @return The ID of the embedding assigned when adding this embedding to the store.
     */
    public String embeddingId() {
        return embeddingId;
    }

    /**
     * Returns the embedding that has been matched.
     * @return The embedding that has been matched.
     */
    public VectorData embedding() {
        return embedding;
    }

    /**
     * Returns the original content that was embedded.
     * @return The original content that was embedded. Typically, this is a {@link com.llmagent.data.segment.TextSegment}.
     */
    public T embedded() {
        return embedded;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EmbeddingMatch<?> that = (EmbeddingMatch<?>) o;
        return Objects.equals(this.score, that.score)
                && Objects.equals(this.embeddingId, that.embeddingId)
                && Objects.equals(this.embedding, that.embedding)
                && Objects.equals(this.embedded, that.embedded);
    }

    @Override
    public int hashCode() {
        return Objects.hash(score, embeddingId, embedding, embedded);
    }

    @Override
    public String toString() {
        return "EmbeddingMatch {" +
                " score = " + score +
                ", embedded = " + embedded +
                ", embeddingId = " + embeddingId +
                ", embedding = " + embedding +
                " }";
    }
}
