package com.llmagent.embedding.dashscope.api;

import com.llmagent.embedding.dashscope.EmbeddingRequest;
import com.llmagent.embedding.dashscope.EmbeddingResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface DashscopeApi {

    @POST("services/embeddings/multimodal-embedding/multimodal-embedding")
    @Headers("Content-Type: application/json")
    Call<EmbeddingResponse> embeddings(@Body EmbeddingRequest request);

}
