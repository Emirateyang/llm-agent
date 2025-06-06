package com.llmagent;

/**
 * used to encapsulate the definitions of fields,
 */
public class FieldDefinition {

    String idFieldName;

    String textFieldName;

    String metadataFieldName;

    String vectorFieldName;

    public FieldDefinition(String idFieldName, String textFieldName, String metadataFieldName, String vectorFieldName) {
        this.idFieldName = idFieldName;
        this.textFieldName = textFieldName;
        this.metadataFieldName = metadataFieldName;
        this.vectorFieldName = vectorFieldName;
    }

    public String getIdFieldName() {
        return idFieldName;
    }

    public String getTextFieldName() {
        return textFieldName;
    }

    public String getMetadataFieldName() {
        return metadataFieldName;
    }

    public String getVectorFieldName() {
        return vectorFieldName;
    }
}
