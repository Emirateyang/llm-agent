package com.llmagent.embedding.doubao.api;

import com.llmagent.embedding.doubao.EmbeddingRequest;
import com.llmagent.embedding.doubao.EmbeddingResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface DoubaoApi {

    @POST("embeddings/multimodal")
    @Headers("Content-Type: application/json")
    Call<EmbeddingResponse> embeddings(@Body EmbeddingRequest request);

}
