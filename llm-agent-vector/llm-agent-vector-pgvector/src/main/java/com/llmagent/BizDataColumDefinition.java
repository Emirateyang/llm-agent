package com.llmagent;

import java.util.Arrays;
import java.util.List;

public class BizDataColumDefinition {
    private final String fullDefinition;
    private final String name;
    private final String type;

    private BizDataColumDefinition(String fullDefinition, String name, String type) {
        this.fullDefinition = fullDefinition;
        this.name = name;
        this.type = type;
    }

    /**
     * transform sql string to BizDataColumDefinition
     * @param sqlDefinition sql definition string
     * @return BizDataColumDefinition
     */
    public static BizDataColumDefinition from(String sqlDefinition) {
        List<String> tokens = Arrays.stream(sqlDefinition.split(" "))
                .filter(s -> !s.isEmpty()).toList();
        if (tokens.size() < 2) {
            throw new IllegalArgumentException("Definition format should be: column type" +
                    " [ NULL | NOT NULL ] [ UNIQUE ] [ DEFAULT value ]");
        }
        String name = tokens.get(0);
        String type = tokens.get(1).toLowerCase();
        return new BizDataColumDefinition(sqlDefinition, name, type);
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
