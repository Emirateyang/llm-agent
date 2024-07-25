package com.llmagent.dify.chat;

import lombok.Data;

import java.util.List;

@Data
public class DifyResponseMetadata {

    private DifyUsage usage;
    private List<RetrieverResource> retrieverResources;
}
