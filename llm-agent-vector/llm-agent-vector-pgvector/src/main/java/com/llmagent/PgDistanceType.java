package com.llmagent;

public enum PgDistanceType {

    EUCLIDEAN_DISTANCE("<->", "vector_l2_ops",
            "SELECT *, embedding <-> ? AS distance FROM %s WHERE embedding <-> ? < ? %s ORDER BY distance LIMIT ? "),

    // NOTE: works only if If vectors are normalized to length 1 (like OpenAI
    // embeddings), use inner product for best performance.
    // The Sentence transformers are NOT normalized:
    // https://github.com/UKPLab/sentence-transformers/issues/233
    NEGATIVE_INNER_PRODUCT("<#>", "vector_ip_ops",
            "SELECT *, (1 + (embedding <#> ?)) AS distance FROM %s WHERE (1 + (embedding <#> ?)) < ? %s ORDER BY distance LIMIT ? "),

    COSINE_DISTANCE("<=>", "vector_cosine_ops",
            "SELECT *, embedding <=> ? AS distance FROM %s WHERE embedding <=> ? < ? %s ORDER BY distance LIMIT ? ");

    public final String operator;

    public final String index;

    public final String similaritySearchSqlTemplate;

    PgDistanceType(String operator, String index, String sqlTemplate) {
        this.operator = operator;
        this.index = index;
        this.similaritySearchSqlTemplate = sqlTemplate;
    }
}
