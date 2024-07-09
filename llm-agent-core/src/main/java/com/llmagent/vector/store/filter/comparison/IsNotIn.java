package com.llmagent.vector.store.filter.comparison;

import com.llmagent.data.Metadata;
import com.llmagent.vector.store.filter.Filter;
import lombok.EqualsAndHashCode;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static com.llmagent.util.NumberComparator.containsAsBigDecimals;
import static com.llmagent.util.ValidationUtil.*;
import static com.llmagent.vector.store.filter.comparison.TypeChecker.ensureTypesAreCompatible;
import static java.util.Collections.unmodifiableSet;

@EqualsAndHashCode
public class IsNotIn implements Filter {

    private final String key;
    private final Collection<?> comparisonValues;

    public IsNotIn(String key, Collection<?> comparisonValues) {
        this.key = ensureNotBlank(key, "key");
        Set<?> copy = new HashSet<>(ensureNotEmpty(comparisonValues, "comparisonValues with key '" + key + "'"));
        comparisonValues.forEach(value -> ensureNotNull(value, "comparisonValue with key '" + key + "'"));
        this.comparisonValues = unmodifiableSet(copy);
    }

    public String key() {
        return key;
    }

    public Collection<?> comparisonValues() {
        return comparisonValues;
    }

    @Override
    public boolean test(Object object) {
        if (!(object instanceof Metadata)) {
            return false;
        }

        Metadata metadata = (Metadata) object;
        if (!metadata.containsKey(key)) {
            return true;
        }

        Object actualValue = metadata.toMap().get(key);
        ensureTypesAreCompatible(actualValue, comparisonValues.iterator().next(), key);

        if (comparisonValues.iterator().next() instanceof Number) {
            return !containsAsBigDecimals(actualValue, comparisonValues);
        }

        return !comparisonValues.contains(actualValue);
    }
}
