package com.llmagent.embedding.onnx;

import ai.djl.huggingface.tokenizers.Encoding;
import ai.onnxruntime.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.*;

import static ai.onnxruntime.OnnxTensor.createTensor;
import static com.llmagent.exception.Exceptions.illegalArgument;
import static com.llmagent.util.ValidationUtil.ensureNotNull;
import static java.nio.LongBuffer.wrap;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;

/**
 * OnnxBertEncoder is designed to encode text using a BERT model.
 * This class utilizes the ONNX runtime for inference and the HuggingFace tokenizer for text preprocessing.
 * The class supports different pooling modes to aggregate the embeddings from the BERT model.
 */
public class OnnxBertEncoder {

    // The maximum sequence length for the BERT model
    private static final int MAX_SEQUENCE_LENGTH = 510;

    // The environment for the ONNX runtime
    private final OrtEnvironment environment;
    private final OrtSession session;
    private final Set<String> expectedInputs;
    // The set of expected inputs for the ONNX runtime
    private final HuggingFaceTokenizer tokenizer;
    // The HuggingFace tokenizer
    private final PoolingMode poolingMode;
    // The pooling mode for the BERT model

    /**
     * initializes an instance of the class by setting up the necessary environment and resources for processing text using a BERT model.
     * The constructor is suitable for small models
     * @param model the ONNX model file
     * @param tokenizer the HuggingFace tokenizer file
     * @param poolingMode the pooling mode for the BERT model
     */
    public OnnxBertEncoder(InputStream model, InputStream tokenizer, PoolingMode poolingMode) {
        try {
            // Get the OrtEnvironment
            this.environment = OrtEnvironment.getEnvironment();
            // Create a session from the model
            this.session = environment.createSession(loadModel(model));
            // Get the input names of the session
            this.expectedInputs = session.getInputNames();
            // Create a tokenizer from the tokenizer input stream
            this.tokenizer = new HuggingFaceTokenizer(tokenizer, singletonMap("padding", "false"));
            // Ensure that the pooling mode is not null
            this.poolingMode = ensureNotNull(poolingMode, "poolingMode");
        } catch (Exception e) {
            // Throw a runtime exception if an exception is caught
            throw new RuntimeException(e);
        }
    }

    /**
     * initializes an instance of the class by setting up the necessary environment and resources for processing text using a BERT model.
     * The constructor is suitable for some models which has external data file. For example, the BAAI/bge-m3
     *
     * @param modelPath the path to the ONNX model file
     * @param modelName the name of the ONNX model file
     * @param tokenizer the name of the HuggingFace tokenizer file
     * @param poolingMode the pooling mode for the BERT model
     */
    public OnnxBertEncoder(String modelPath, String modelName, String tokenizer, PoolingMode poolingMode) {

        try {
            this.environment = OrtEnvironment.getEnvironment();
            this.session = environment.createSession(modelPath + modelName);
            this.expectedInputs = session.getInputNames();
            this.tokenizer = new HuggingFaceTokenizer(Paths.get(modelPath + tokenizer), singletonMap("padding", "false"));
            this.poolingMode = ensureNotNull(poolingMode, "poolingMode");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public OnnxBertEncoder(OrtEnvironment environment, OrtSession session, InputStream tokenizer, PoolingMode poolingMode) {
        try {
            this.environment = environment;
            this.session = session;
            this.expectedInputs = session.getInputNames();
            this.tokenizer = new HuggingFaceTokenizer(tokenizer, singletonMap("padding", "false"));
            this.poolingMode = ensureNotNull(poolingMode, "poolingMode");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static class EmbeddingAndTokenCount {

        public float[] embedding;
        public int tokenCount;

        EmbeddingAndTokenCount(float[] embedding, int tokenCount) {
            this.embedding = embedding;
            this.tokenCount = tokenCount;
        }
    }

    /**
     * Encodes the input text using the BERT model and returns the embedding and token count.
     * @param text the input text to encode
     * @return the embedding and token count
     */
    public EmbeddingAndTokenCount embed(String text) {
        // Tokenize the input text
        List<String> tokens = tokenizer.tokenize(text);
        // Partition the tokens into chunks of MAX_SEQUENCE_LENGTH
        List<List<String>> partitions = partition(tokens, MAX_SEQUENCE_LENGTH);

        List<float[]> embeddings = new ArrayList<>();
        for (List<String> partition : partitions) {
            try (OrtSession.Result result = encode(partition)) {
                float[] embedding = toEmbedding(result);
                // Get the embedding for the partition
                embeddings.add(embedding);
                // Add the embedding to the list
            } catch (OrtException e) {
                throw new RuntimeException(e);
            }
        }

        // List to store the weights for each partition
        List<Integer> weights = partitions.stream()
                .map(List::size)
                .collect(toList());

        // Normalize the weighted average of the embeddings
        float[] embedding = normalize(weightedAverage(embeddings, weights));

        // Return the embedding and token count
        return new EmbeddingAndTokenCount(embedding, tokens.size());
    }

    static List<List<String>> partition(List<String> tokens, int partitionSize) {
    // Method to partition the tokens into chunks of partitionSize
        List<List<String>> partitions = new ArrayList<>();
        int from = 1; // Skip the first (CLS) token

        while (from < tokens.size() - 1) { // Skip the last (SEP) token
            int to = from + partitionSize;

            if (to >= tokens.size() - 1) {
                to = tokens.size() - 1;
            } else {
                // ensure we don't split word across partitions
                while (tokens.get(to).startsWith("##")) {
                    to--;
                }
            }

            partitions.add(tokens.subList(from, to));

            from = to;
        }

        return partitions;
    }

    /**
     * Encodes a list of tokens into a format suitable for input into an ONNX model.
     *
     * @param tokens a list of strings representing the tokens to be encoded
     * @return an OrtSession.Result object containing the encoded tensors
     * @throws OrtException if an error occurs during the encoding process
     */
    private OrtSession.Result encode(List<String> tokens) throws OrtException {
        Encoding encoding = tokenizer.encode(toText(tokens), true, false);
        // Encode the tokens

        long[] inputIds = encoding.getIds();
        // Get the input IDs, attention mask, and token type IDs
        long[] attentionMask = encoding.getAttentionMask();
        long[] tokenTypeIds = encoding.getTypeIds();

        // Create the shape for the tensors
        long[] shape = {1, inputIds.length};

        try (
            // Create the tensors
            OnnxTensor inputIdsTensor = createTensor(environment, wrap(inputIds), shape);
            OnnxTensor attentionMaskTensor = createTensor(environment, wrap(attentionMask), shape);
            OnnxTensor tokenTypeIdsTensor = createTensor(environment, wrap(tokenTypeIds), shape)
        ) {
            Map<String, OnnxTensor> inputs = new HashMap<>();
            // Create the input map
            inputs.put("input_ids", inputIdsTensor);
            inputs.put("attention_mask", attentionMaskTensor);

            // Add the token type IDs tensor if it is expected
            if (expectedInputs.contains("token_type_ids")) {
                inputs.put("token_type_ids", tokenTypeIdsTensor);
            }

            return session.run(inputs);
        }
    }

    /**
     * Converts a list of tokens back to text.
     * If the tokens match the original text after tokenization and special tokens are removed, the original text is returned.
     * Otherwise, the tokens are joined into a string.
     *
     * @param tokens a list of tokens to be converted to text
     * @return the original text if the tokens match the original text after tokenization and special tokens are removed, otherwise the tokens joined into a string
     */
    private String toText(List<String> tokens) {
    // Method to convert the tokens to text

        String text = tokenizer.buildSentence(tokens);
        // Build the sentence from the tokens

        List<String> tokenized = tokenizer.tokenize(text);
        // Tokenize the text
        List<String> tokenizedWithoutSpecialTokens = new LinkedList<>(tokenized);
        // Remove the special tokens
        tokenizedWithoutSpecialTokens.remove(0);
        tokenizedWithoutSpecialTokens.remove(tokenizedWithoutSpecialTokens.size() - 1);

        if (tokenizedWithoutSpecialTokens.equals(tokens)) {
        // Check if the tokenized text matches the original tokens
            return text;
        } else {
            return String.join("", tokens);
        }
    }

    private float[] toEmbedding(OrtSession.Result result) throws OrtException {
    // Method to convert the result to an embedding
        float[][] vectors = ((float[][][]) result.get(0).getValue())[0];
        // Get the vectors from the result
        return pool(vectors);
        // Pool the vectors
    }

    private float[] pool(float[][] vectors) {
    // Method to pool the vectors
        switch (poolingMode) {
        // Check the pooling mode
            case CLS:
                return clsPool(vectors);
            case MEAN:
                return meanPool(vectors);
            default:
                throw illegalArgument("Unknown pooling mode: " + poolingMode);
        }
    }

    /**
     * pool the vectors using the CLS token
     * @param vectors the vectors to pool
     */
    private static float[] clsPool(float[][] vectors) {
        return vectors[0];
    }

    /**
     * pool the vectors using the mean token
     * @param vectors the vectors to pool
     */
    private static float[] meanPool(float[][] vectors) {
        int numVectors = vectors.length;
        // Get the number of vectors and the length of each vector
        int vectorLength = vectors[0].length;

        float[] averagedVector = new float[vectorLength];
        // Create the averaged vector

        for (float[] vector : vectors) {
        // Loop through each vector
            // Loop through each element in the vector
            for (int j = 0; j < vectorLength; j++) {
                averagedVector[j] += vector[j];
            }
        }

        // Normalize the averaged vector
        for (int j = 0; j < vectorLength; j++) {
            averagedVector[j] /= numVectors;
        }

        return averagedVector;
    }

    // Method to calculate the weighted average of the embeddings
    private float[] weightedAverage(List<float[]> embeddings, List<Integer> weights) {
        // Check if there is only one embedding
        if (embeddings.size() == 1) {
            return embeddings.get(0);
        }

        // Get the dimensions of the embeddings
        int dimensions = embeddings.get(0).length;

        // Create the averaged embedding
        float[] averagedEmbedding = new float[dimensions];
        int totalWeight = 0;

        // Loop through each embedding
        for (int i = 0; i < embeddings.size(); i++) {
            // Get the weight for the embedding
            int weight = weights.get(i);
            totalWeight += weight;

            // Loop through each element in the embedding
            for (int j = 0; j < dimensions; j++) {
                averagedEmbedding[j] += embeddings.get(i)[j] * weight;
            }
        }

        // Normalize the averaged embedding
        for (int j = 0; j < dimensions; j++) {
            averagedEmbedding[j] /= totalWeight;
        }

        return averagedEmbedding;
    }

    // Method to normalize the vector
    private static float[] normalize(float[] vector) {

        // Calculate the sum of the squares of the elements in the vector
        float sumSquare = 0;
        for (float v : vector) {
            sumSquare += v * v;
        }
        // Calculate the norm of the vector
        float norm = (float) Math.sqrt(sumSquare);

        // Create the normalized vector
        float[] normalizedVector = new float[vector.length];
        for (int i = 0; i < vector.length; i++) {
            normalizedVector[i] = vector[i] / norm;
        }

        return normalizedVector;
    }

    // Method to count the number of tokens in the text
    int countTokens(String text) {
        return tokenizer.tokenize(text).size();
    }

    /**
     * load the model from the input stream
     * @param modelInputStream the input stream of the model
     * @return the model
     */
    private byte[] loadModel(InputStream modelInputStream) {
        try (
                InputStream inputStream = modelInputStream;
                ByteArrayOutputStream buffer = new ByteArrayOutputStream()
        ) {
            int nRead;
            byte[] data = new byte[1024];

            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }

            buffer.flush();
            return buffer.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
