package com.llmagent.embedding.http;

import retrofit2.Call;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Function;

public class AsyncRequestExecutor<Resp, RespContent> {

    private final Call<Resp> call;
    private final Function<Resp, RespContent> responseContentExtractor;

    AsyncRequestExecutor(Call<Resp> call,
                         Function<Resp, RespContent> responseContentExtractor) {
        this.call = call;
        this.responseContentExtractor = responseContentExtractor;
    }

    AsyncResponseHandling onResponse(Consumer<RespContent> responseHandler) {
        return new AsyncResponseHandling() {

            @Override
            public ErrorHandling onError(Consumer<Throwable> errorHandler) {
                return new ErrorHandling() {

                    @Override
                    public ResponseHandle execute() {
                        try {
                            retrofit2.Response<Resp> retrofitResponse = call.execute();
                            if (retrofitResponse.isSuccessful()) {
                                Resp response = retrofitResponse.body();
                                RespContent responseContent = responseContentExtractor.apply(response);
                                responseHandler.accept(responseContent);
                            } else {
                                errorHandler.accept(ExceptionUtil.toException(retrofitResponse));
                            }
                        } catch (IOException e) {
                            errorHandler.accept(e);
                        }
                        return new ResponseHandle();
                    }
                };
            }

            @Override
            public ErrorHandling ignoreErrors() {
                return new ErrorHandling() {

                    @Override
                    public ResponseHandle execute() {
                        try {
                            retrofit2.Response<Resp> retrofitResponse = call.execute();
                            if (retrofitResponse.isSuccessful()) {
                                Resp response = retrofitResponse.body();
                                RespContent responseContent = responseContentExtractor.apply(response);
                                responseHandler.accept(responseContent);
                            }
                        } catch (IOException e) {
                            // intentionally ignoring, because user called ignoreErrors()
                        }
                        return new ResponseHandle();
                    }
                };
            }
        };
    }
}
