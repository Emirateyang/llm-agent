package com.llmagent;

public enum PgIndexType {
    /**
     * Performs exact nearest neighbor search, which provides perfect recall.
     */
    NONE,
    /**
     * An IVFFlat index divides vectors into lists, and then searches a subset of
     * those lists that are closest to the query vector. It has faster build times and
     * uses less memory than HNSW, but has lower query performance (in terms of
     * speed-recall tradeoff).
     */
    IVFFLAT,
    /**
     * An HNSW index creates a multilayer graph. It has slower build times and uses
     * more memory than IVFFlat, but has better query performance (in terms of
     * speed-recall tradeoff). There’s no training step like IVFFlat, so the index can
     * be created without any data in the table.
     */
    HNSW;
}
