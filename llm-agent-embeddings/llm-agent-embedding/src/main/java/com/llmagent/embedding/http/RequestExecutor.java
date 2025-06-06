package com.llmagent.embedding.http;

import okhttp3.OkHttpClient;
import retrofit2.Call;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class RequestExecutor<Req, Resp, RespContent>
        implements SyncOrAsync<RespContent> {

    private final Call<Resp> call;
    private final Function<Resp, RespContent> responseContentExtractor;

    private final OkHttpClient okHttpClient;
    private final String endpointUrl;
    private final Supplier<Req> requestWithStreamSupplier;
    private final Class<Resp> responseClass;
    private final Function<Resp, RespContent> streamEventContentExtractor;
    private final boolean logStreamingResponses;

    public RequestExecutor(Call<Resp> call,
                           Function<Resp, RespContent> responseContentExtractor,

                           OkHttpClient okHttpClient,
                           String endpointUrl,
                           Supplier<Req> requestWithStreamSupplier,
                           Class<Resp> responseClass,
                           Function<Resp, RespContent> streamEventContentExtractor,
                           boolean logStreamingResponses
    ) {
        this.call = call;
        this.responseContentExtractor = responseContentExtractor;

        this.okHttpClient = okHttpClient;
        this.endpointUrl = endpointUrl;
        this.requestWithStreamSupplier = requestWithStreamSupplier;
        this.responseClass = responseClass;
        this.streamEventContentExtractor = streamEventContentExtractor;
        this.logStreamingResponses = logStreamingResponses;
    }

    public RequestExecutor(Call<Resp> call, Function<Resp, RespContent> responseContentExtractor) {
        this.call = call;
        this.responseContentExtractor = responseContentExtractor;

        this.okHttpClient = null;
        this.endpointUrl = null;
        this.requestWithStreamSupplier = null;
        this.responseClass = null;
        this.streamEventContentExtractor = null;
        this.logStreamingResponses = false;
    }

    @Override
    public RespContent execute() {
        return new SyncRequestExecutor<>(call, responseContentExtractor).execute();
    }

    @Override
    public AsyncResponseHandling onResponse(Consumer<RespContent> responseHandler) {

        return new AsyncRequestExecutor<>(call, responseContentExtractor).onResponse(responseHandler);
    }

}
