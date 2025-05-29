package com.llmagent.embedding.http;

import retrofit2.Call;

import java.io.IOException;
import java.util.function.Function;

public class SyncRequestExecutor<Resp, RespContent> {

    private final Call<Resp> call;
    private final Function<Resp, RespContent> responseContentExtractor;

    SyncRequestExecutor(Call<Resp> call,
                        Function<Resp, RespContent> responseContentExtractor) {
        this.call = call;
        this.responseContentExtractor = responseContentExtractor;
    }

    RespContent execute() {
        try {
            retrofit2.Response<Resp> retrofitResponse = call.execute();
            if (retrofitResponse.isSuccessful()) {
                Resp response = retrofitResponse.body();
                return responseContentExtractor.apply(response);
            } else {
                throw ExceptionUtil.toException(retrofitResponse);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
