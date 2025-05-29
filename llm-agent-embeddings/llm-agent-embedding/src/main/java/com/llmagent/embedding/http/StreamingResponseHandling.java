package com.llmagent.embedding.http;

public interface StreamingResponseHandling extends AsyncResponseHandling {

    StreamingCompletionHandling onComplete(Runnable streamingCompletionCallback);
}
