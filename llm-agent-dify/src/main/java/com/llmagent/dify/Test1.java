package com.llmagent.dify;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

public class Test1 {
    public static void main(String[] args) {
        String content = "{\"arxiv_search\": \"Published: 2025-04-23\\nTitle\"}";
        content = content.replace("\\n", " ")
                .replace("\\r", " ")
                .replace("\\t", " ");
        JSONObject jsonObject = JSON.parseObject(content);
        System.out.println(jsonObject.getJSONObject("arxiv_search"));
    }
}
