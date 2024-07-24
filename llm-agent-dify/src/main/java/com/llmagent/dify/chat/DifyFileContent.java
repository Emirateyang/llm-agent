package com.llmagent.dify.chat;

import lombok.*;

@Builder
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class DifyFileContent {

    private String type;
    //  remote_url only for using api mode
    private String transferMethod = "remote_url";
    private String url;
}
