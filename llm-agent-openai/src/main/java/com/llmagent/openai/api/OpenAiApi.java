package com.llmagent.openai.api;

import com.llmagent.openai.chat.ChatCompletionRequest;
import com.llmagent.openai.chat.ChatCompletionResponse;
import com.llmagent.openai.completion.CompletionRequest;
import com.llmagent.openai.completion.CompletionResponse;
import com.llmagent.openai.embedding.EmbeddingRequest;
import com.llmagent.openai.embedding.EmbeddingResponse;
import com.llmagent.openai.image.GenerateImagesRequest;
import com.llmagent.openai.image.GenerateImagesResponse;
import com.llmagent.openai.moderation.ModerationRequest;
import com.llmagent.openai.moderation.ModerationResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface OpenAiApi {
    @POST("completions")
    @Headers("Content-Type: application/json")
    Call<CompletionResponse> completions(@Body CompletionRequest request, @Query("api-version") String apiVersion);

    @POST("chat/completions")
    @Headers("Content-Type: application/json")
    Call<ChatCompletionResponse> chatCompletions(
            @Body ChatCompletionRequest request,
            @Query("api-version") String apiVersion
    );

    @POST("embeddings")
    @Headers("Content-Type: application/json")
    Call<EmbeddingResponse> embeddings(@Body EmbeddingRequest request, @Query("api-version") String apiVersion);

    @POST("moderations")
    @Headers("Content-Type: application/json")
    Call<ModerationResponse> moderations(@Body ModerationRequest request, @Query("api-version") String apiVersion);

    @POST("images/generations")
    @Headers({ "Content-Type: application/json" })
    Call<GenerateImagesResponse> imagesGenerations(
            @Body GenerateImagesRequest request,
            @Query("api-version") String apiVersion
    );
}
