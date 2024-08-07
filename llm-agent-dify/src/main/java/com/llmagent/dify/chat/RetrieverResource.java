package com.llmagent.dify.chat;

import lombok.Data;

@Data
public class RetrieverResource {

    private Integer position;
    private String datasetId;
    private String datasetName;
    private String documentId;
    private String documentName;
    private String segmentId;
    private Double score;
    private String content;
}
