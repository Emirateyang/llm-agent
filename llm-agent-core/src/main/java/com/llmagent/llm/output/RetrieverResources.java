package com.llmagent.llm.output;

public class RetrieverResources {

    private Integer position;
    private String datasetId;
    private String datasetName;
    private String documentId;
    private String documentName;
    private String segmentId;
    private Double score;
    private String content;

    public RetrieverResources(Integer position, String datasetId, String datasetName, String documentId,
                              String documentName, String segmentId, Double score, String content) {
        this.position = position;
        this.datasetId = datasetId;
        this.datasetName = datasetName;
        this.documentId = documentId;
        this.documentName = documentName;
        this.segmentId = segmentId;
        this.score = score;
        this.content = content;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public String getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(String datasetId) {
        this.datasetId = datasetId;
    }

    public String getDatasetName() {
        return datasetName;
    }

    public void setDatasetName(String datasetName) {
        this.datasetName = datasetName;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getDocumentName() {
        return documentName;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }

    public String getSegmentId() {
        return segmentId;
    }

    public void setSegmentId(String segmentId) {
        this.segmentId = segmentId;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
