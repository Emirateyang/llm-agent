package com.llmagent.embedding.siliconflow.api;

import com.llmagent.embedding.siliconflow.EmbeddingRequest;
import com.llmagent.embedding.siliconflow.EmbeddingResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface SiliconflowApi {

    @POST("embeddings")
    @Headers("Content-Type: application/json")
    Call<EmbeddingResponse> embeddings(@Body EmbeddingRequest request);

}
