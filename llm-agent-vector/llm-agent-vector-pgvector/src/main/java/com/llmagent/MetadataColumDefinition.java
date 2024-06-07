package com.llmagent;

import com.llmagent.util.StringUtil;

import java.util.Arrays;
import java.util.List;

public class MetadataColumDefinition {
    private final String fullDefinition;
    private final String name;
    private final String type;

    private MetadataColumDefinition(String fullDefinition, String name, String type) {
        this.fullDefinition = fullDefinition;
        this.name = name;
        this.type = type;
    }

    /**
     * transform sql string to MetadataColumDefinition
     * @param sqlDefinition sql definition string
     * @return MetadataColumDefinition
     */
    public static MetadataColumDefinition from(String sqlDefinition) {
        String fullDefinition = StringUtil.hasText(sqlDefinition) ? sqlDefinition :"Metadata column definition";
        List<String> tokens = Arrays.stream(fullDefinition.split(" "))
                .filter(s -> !s.isEmpty()).toList();
        if (tokens.size() < 2) {
            throw new IllegalArgumentException("Definition format should be: column type" +
                    " [ NULL | NOT NULL ] [ UNIQUE ] [ DEFAULT value ]");
        }
        String name = tokens.get(0);
        String type = tokens.get(1).toLowerCase();
        return new MetadataColumDefinition(fullDefinition, name, type);
    }

    public String getFullDefinition() {
        return fullDefinition;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }
}
