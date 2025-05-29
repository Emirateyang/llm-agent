package com.llmagent;

import com.llmagent.embedding.doubao.EmbeddingModelName;
import com.llmagent.embedding.doubao.EmbeddingOutput;
import com.llmagent.embedding.doubao.MultimodalEmbeddingModel;
import com.llmagent.llm.output.LlmResponse;

public class Test {
    public static void main(String[] args) throws Exception {
//        DefaultMultimodalEmbeddingClient client = DefaultMultimodalEmbeddingClient.builder()
//                .apiKey("sk-c059fb45c25d435d82478f9df2e5cb40")
//                .build();
//        EmbeddingResponse response = client.embeddingImage("https://dev.oss.leziedu.com/aiTourGuide/images/search/0a6879cc776b4f70bca0c6da98dd6f8d/1743240059442.jpg");
//        System.out.println(response);

        MultimodalEmbeddingModel model = MultimodalEmbeddingModel.builder()
                .modelName(EmbeddingModelName.MULTI_MODAL_EMBEDDING_250328)
                .apiKey("73a74f56-4826-4a4f-ba91-428c5c6fce83")
                .build();

        LlmResponse<EmbeddingOutput> embeddings = model.embedImage("https://dev.oss.leziedu.com/aiTourGuide/images/search/0a6879cc776b4f70bca0c6da98dd6f8d/1743240059442.jpg");
        if (embeddings == null || embeddings.content() == null) {
            System.out.println("No embeddings");
        }

        System.out.println("embeddings found :" + embeddings.content().embedding());
    }
}
