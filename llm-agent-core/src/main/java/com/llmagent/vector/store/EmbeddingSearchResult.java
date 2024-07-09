package com.llmagent.vector.store;

import java.util.List;

public class EmbeddingSearchResult<T> {

    private final List<EmbeddingMatch<T>> matches;

    public EmbeddingSearchResult(List<EmbeddingMatch<T>> matches) {
        this.matches = matches;
    }

    public List<EmbeddingMatch<T>> matches() {
        return matches;
    }
}
