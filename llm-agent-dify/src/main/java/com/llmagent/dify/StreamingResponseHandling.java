package com.llmagent.dify;

public interface StreamingResponseHandling extends AsyncResponseHandling {

    StreamingCompletionHandling onComplete(Runnable streamingCompletionCallback);
}
