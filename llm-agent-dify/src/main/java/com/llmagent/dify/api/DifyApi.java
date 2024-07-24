package com.llmagent.dify.api;

import com.llmagent.dify.chat.DifyChatCompletionResponse;
import com.llmagent.dify.chat.DifyMessageRequest;
import com.llmagent.dify.chat.DifyStreamingChatCompletionResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface DifyApi {

    @POST("chat-messages")
    @Headers("Content-Type: application/json")
    Call<DifyChatCompletionResponse> chatCompletion(@Body DifyMessageRequest request);


    @POST("chat-messages")
    @Headers("Content-Type: application/json")
    Call<DifyStreamingChatCompletionResponse> streamingChatCompletion(@Body DifyMessageRequest request);
}
