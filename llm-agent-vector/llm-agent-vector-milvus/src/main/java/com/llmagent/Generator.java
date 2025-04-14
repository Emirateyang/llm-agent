package com.llmagent;

import com.google.gson.JsonObject;
import com.llmagent.util.UUIDUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Generator {
    static List<String> generateRandomIds(int size) {
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            ids.add(UUIDUtil.simpleUUID());
        }
        return ids;
    }

    static List<String> generateEmptyScalars(int size) {
        String[] arr = new String[size];
        Arrays.fill(arr, "");
        return Arrays.asList(arr);
    }

    static List<JsonObject> generateEmptyJsons(int size) {
        List<JsonObject> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            list.add(new JsonObject());
        }
        return list;
    }
}
