package com.llmagent.embedding.onnx;

import com.llmagent.data.segment.TextSegment;
import com.llmagent.llm.embedding.DimensionAwareEmbeddingModel;
import com.llmagent.llm.output.LlmResponse;
import com.llmagent.llm.output.TokenUsage;
import com.llmagent.vector.store.VectorData;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static com.llmagent.util.ObjectUtil.getOrDefault;
import static com.llmagent.util.ValidationUtil.ensureNotEmpty;
import static java.nio.file.Files.newInputStream;
import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 *
 * It provides methods for loading a model from a jar file or the file system, embedding segments of text, and handling parallel processing.
 * The class uses an executor for parallel processing and provides methods for creating a default executor or loading a model from different sources.
 * Used to create Encoder {@link OnnxBertEncoder}
 */
public abstract class ProcessEmbeddingModel extends DimensionAwareEmbeddingModel {

    // Executor to be used for parallel processing
    private final Executor executor;

    // Constructor to initialize the executor
    protected ProcessEmbeddingModel(Executor executor) {
        this.executor = getOrDefault(executor, this::createDefaultExecutor);
    }

    // Method to create a default executor
    private Executor createDefaultExecutor() {
        // Get the number of available processors
        int threadPoolSize = Runtime.getRuntime().availableProcessors();
        // Create a ThreadPoolExecutor with the number of available processors
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                threadPoolSize, threadPoolSize,
                1, SECONDS,
                new LinkedBlockingQueue<>()
        );
        // Allow core threads to time out
        threadPoolExecutor.allowCoreThreadTimeOut(true);
        return threadPoolExecutor;
    }

    // Method to load the model from a jar file
    protected static OnnxBertEncoder loadFromJar(String modelFileName, String tokenizerFileName, PoolingMode poolingMode) {
        // Get the input stream for the model and tokenizer from the class loader
        InputStream model = Thread.currentThread().getContextClassLoader().getResourceAsStream(modelFileName);
        InputStream tokenizer = Thread.currentThread().getContextClassLoader().getResourceAsStream(tokenizerFileName);
        // Create a new OnnxBertEncoder with the input streams and pooling mode
        return new OnnxBertEncoder(model, tokenizer, poolingMode);
    }

    // Method to load the model from the file system
    protected static OnnxBertEncoder loadFromFileSystem(Path pathToModel, Path pathToTokenizer, PoolingMode poolingMode) {
        try {
            // Create a new OnnxBertEncoder with the input streams and pooling mode
            return new OnnxBertEncoder(newInputStream(pathToModel), newInputStream(pathToTokenizer), poolingMode);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Method to load the model from the file system with a tokenizer input stream
    protected static OnnxBertEncoder loadFromFileSystem(Path pathToModel, InputStream tokenizer, PoolingMode poolingMode) {
        try {
            // Create a new OnnxBertEncoder with the input streams and pooling mode
            return new OnnxBertEncoder(newInputStream(pathToModel), tokenizer, poolingMode);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Method to load the model from the file system with a path to the model and tokenizer
    protected static OnnxBertEncoder loadFromFileSystemPath(String modelPath, String modelFileName, String tokenizerFileName,  PoolingMode poolingMode) {
        // Create a new OnnxBertEncoder with the path to the model and tokenizer and pooling mode
        return new OnnxBertEncoder(modelPath, modelFileName, tokenizerFileName, poolingMode);
    }

    // Abstract method to load the model
    protected abstract OnnxBertEncoder model();

    // Method to embed all the segments
    @Override
    public LlmResponse<List<VectorData>> embedAll(List<TextSegment> segments) {
        // Ensure that the segments are not empty
        ensureNotEmpty(segments, "segments");
        // If there is only one segment, embed it in the same thread
        if (segments.size() == 1) {
            return embedInTheSameThread(segments.get(0));
        } else {
            // Otherwise, parallelize the embedding
            return parallelizeEmbedding(segments);
        }
    }

    // Method to embed a single segment in the same thread
    private LlmResponse<List<VectorData>> embedInTheSameThread(TextSegment segment) {
        // Embed the segment and get the embedding and token count
        OnnxBertEncoder.EmbeddingAndTokenCount embeddingAndTokenCount = model().embed(segment.text());
        // Return the embedding and token count
        return LlmResponse.from(
                singletonList(VectorData.from(embeddingAndTokenCount.embedding)),
                new TokenUsage(embeddingAndTokenCount.tokenCount - 2) // do not count special tokens [CLS] and [SEP])
        );
    }

    // Method to parallelize the embedding of multiple segments
    private LlmResponse<List<VectorData>> parallelizeEmbedding(List<TextSegment> segments) {
        // Create a list of futures for each segment
        List<CompletableFuture<OnnxBertEncoder.EmbeddingAndTokenCount>> futures = segments.stream()
                .map(segment -> supplyAsync(() -> model().embed(segment.text()), executor)).toList();

        int inputTokenCount = 0;
        List<VectorData> embeddings = new ArrayList<>();

        // Get the embedding and token count for each future
        for (CompletableFuture<OnnxBertEncoder.EmbeddingAndTokenCount> future : futures) {
            try {
                OnnxBertEncoder.EmbeddingAndTokenCount embeddingAndTokenCount = future.get();
                embeddings.add(VectorData.from(embeddingAndTokenCount.embedding));
                inputTokenCount += embeddingAndTokenCount.tokenCount - 2; // do not count special tokens [CLS] and [SEP]
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        // Return the embeddings and token count
        return LlmResponse.from(embeddings, new TokenUsage(inputTokenCount));
    }
}
