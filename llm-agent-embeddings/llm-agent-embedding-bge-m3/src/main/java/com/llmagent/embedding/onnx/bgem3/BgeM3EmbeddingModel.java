package com.llmagent.embedding.onnx.bgem3;

import com.llmagent.embedding.onnx.OnnxBertEncoder;
import com.llmagent.embedding.onnx.PoolingMode;
import com.llmagent.embedding.onnx.ProcessEmbeddingModel;

import java.util.concurrent.Executor;

/**
 * BAAI bge-m3 embedding model that runs within your Java application's process.
 * <p>
 * Maximum length of text (in tokens) that can be embedded at once: unlimited.
 * However, while you can embed very long texts, the quality of the embedding degrades as the text lengthens.
 * It is recommended to embed segments of no more than 512 tokens long.
 * <p>
 * Embedding dimensions: 1024
 * <p> <p>
 * Uses an {@link Executor} to parallelize the embedding process.
 * By default, uses a cached thread pool with the number of threads equal to the number of available processors.
 * Threads are cached for 1 second.
 * <p>
 * More details <a href="https://huggingface.co/BAAI/bge-m3">here</a>
 * <br>
 * **IMPORTANT USAGE REQUIREMENT:**
 * Due to the nature of large models and the external data format, this class loads the model directly from the filesystem using the ONNX Runtime Java API.
 * It cannot load the model from classpath resources bundled within a JAR.
 * </p>
 * <p>
 * Users of this class **must** provide the path to the **directory** containing
 * the ONNX model files during instantiation. This directory **must** contain:
 * </p>
 * <ol>
 * <li>The main model structure file (e.g., {@code model.onnx})</li>
 * <li>The corresponding external data file(s) (e.g., {@code model.onnx_data})</li>
 * </ol>
 * <p>
 * The ONNX Runtime will automatically look for the external data file(s) within
 * the same directory as the specified {@code .onnx} file when the session is created.
 * Ensure the application has the necessary read permissions for these files.
 * </p>
 */
public class BgeM3EmbeddingModel extends ProcessEmbeddingModel {

    private final OnnxBertEncoder model;
    private static final String DEFAULT_MODEL_FILENAME = "model.onnx";
    private static final String DEFAULT_TOKENIZER_FILENAME = "tokenizer.json";

    /**
     * Creates an instance of an {@code BgeM3EmbeddingModel}.
     * Uses a cached thread pool with the number of threads equal to the number of available processors.
     * Threads are cached for 1 second.
     */
    public BgeM3EmbeddingModel(String modelPath) {
        this(modelPath, null);
    }

    /**
     * Creates an instance of an {@code BgeSmallZhV15EmbeddingModel}.
     *
     * @param executor The executor to use to parallelize the embedding process.
     */
    public BgeM3EmbeddingModel(String modelPath, Executor executor) {
        super(executor);

        this.model = loadFromFileSystemPath(
                modelPath,
                DEFAULT_MODEL_FILENAME,
                DEFAULT_TOKENIZER_FILENAME,
                PoolingMode.CLS
        );
    }


    @Override
    protected OnnxBertEncoder model() {
        return this.model;
    }

    @Override
    protected Integer knownDimension() {
        return 1024;
    }
}
