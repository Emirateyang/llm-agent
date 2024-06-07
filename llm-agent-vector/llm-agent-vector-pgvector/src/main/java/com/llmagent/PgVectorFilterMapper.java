package com.llmagent;

import com.llmagent.vector.store.filter.Filter;
import com.llmagent.vector.store.filter.comparison.IsEqualTo;
import com.llmagent.vector.store.filter.comparison.IsNotEqualTo;
import com.llmagent.vector.store.filter.logical.And;
import com.llmagent.vector.store.filter.logical.Not;
import com.llmagent.vector.store.filter.logical.Or;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class PgVectorFilterMapper {
    static final Map<Class<?>, String> SQL_TYPE_MAP = Stream.of(
                    new AbstractMap.SimpleEntry<>(Integer.class, "int"),
                    new AbstractMap.SimpleEntry<>(Long.class, "bigint"),
                    new AbstractMap.SimpleEntry<>(Float.class, "float"),
                    new AbstractMap.SimpleEntry<>(Double.class, "float8"),
                    new AbstractMap.SimpleEntry<>(String.class, "text"),
                    new AbstractMap.SimpleEntry<>(Boolean.class, "boolean"),
                    // Default
                    new AbstractMap.SimpleEntry<>(Object.class, "text"))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    public String map(Filter filter) {
        if (filter instanceof IsEqualTo) {
            return mapEqual((IsEqualTo) filter);
        } else if (filter instanceof IsNotEqualTo) {
            return mapNotEqual((IsNotEqualTo) filter);
        } else if (filter instanceof And) {
            return mapAnd((And) filter);
        } else if (filter instanceof Not) {
            return mapNot((Not) filter);
        } else if (filter instanceof Or) {
            return mapOr((Or) filter);
        } else {
            throw new UnsupportedOperationException("Unsupported filter type: " + filter.getClass().getName());
        }
    }

    private String mapEqual(IsEqualTo isEqualTo) {
        String key = formatKey(isEqualTo.key(), isEqualTo.comparisonValue().getClass());
        return String.format("%s is not null and %s = %s", key, key,
                formatValue(isEqualTo.comparisonValue()));
    }

    private String mapNotEqual(IsNotEqualTo isNotEqualTo) {
        String key = formatKey(isNotEqualTo.key(), isNotEqualTo.comparisonValue().getClass());
        return String.format("%s is null or %s != %s", key, key,
                formatValue(isNotEqualTo.comparisonValue()));
    }


    private String mapAnd(And and) {
        return String.format("%s and %s", map(and.left()), map(and.right()));
    }

    private String mapNot(Not not) {
        return String.format("not(%s)", map(not.expression()));
    }

    private String mapOr(Or or) {
        return String.format("(%s or %s)", map(or.left()), map(or.right()));
    }

    abstract String formatKey(String key, Class<?> valueType);

    abstract String formatKeyAsString(String key);

    String formatValue(Object value) {
        if (value instanceof String) {
            return "'" + value + "'";
        } else {
            return value.toString();
        }
    }

    String formatValuesAsString(Collection<?> values) {
        return "(" + values.stream().map(v -> String.format("'%s'", v))
                .collect(Collectors.joining(",")) + ")";
    }
}
