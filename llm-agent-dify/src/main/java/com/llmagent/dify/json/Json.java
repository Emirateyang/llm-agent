package com.llmagent.dify.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import static com.google.gson.FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES;

public class Json {
    public static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(LOWER_CASE_WITH_UNDERSCORES)
//            .registerTypeAdapterFactory(ASSISTANT_MESSAGE_TYPE_ADAPTER_FACTORY)
            .registerTypeHierarchyAdapter(Enum.class, new EnumSerializer())
            .registerTypeHierarchyAdapter(Enum.class, new EnumDeserializer())
            .setPrettyPrinting()
            .create();

    public static String toJson(Object o) {
        return GSON.toJson(o);
    }

    public static <T> T fromJson(String json, Class<T> type) {
        return GSON.fromJson(json, type);
    }
}
