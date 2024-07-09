package com.llmagent.vector.store.filter.comparison;

import com.llmagent.data.Metadata;
import com.llmagent.vector.store.filter.Filter;
import lombok.EqualsAndHashCode;

import static com.llmagent.util.NumberComparator.compareAsBigDecimals;
import static com.llmagent.util.ValidationUtil.ensureNotBlank;
import static com.llmagent.util.ValidationUtil.ensureNotNull;
import static com.llmagent.vector.store.filter.comparison.TypeChecker.ensureTypesAreCompatible;

@EqualsAndHashCode
public class IsLessThanOrEqualTo implements Filter {

    private final String key;
    private final Comparable<?> comparisonValue;

    public IsLessThanOrEqualTo(String key, Comparable<?> comparisonValue) {
        this.key = ensureNotBlank(key, "key");
        this.comparisonValue = ensureNotNull(comparisonValue, "comparisonValue with key '" + key + "'");
    }

    public String key() {
        return key;
    }

    public Comparable<?> comparisonValue() {
        return comparisonValue;
    }

    @Override
    public boolean test(Object object) {
        if (!(object instanceof Metadata)) {
            return false;
        }

        Metadata metadata = (Metadata) object;
        if (!metadata.containsKey(key)) {
            return false;
        }

        Object actualValue = metadata.toMap().get(key);
        ensureTypesAreCompatible(actualValue, comparisonValue, key);

        if (actualValue instanceof Number) {
            return compareAsBigDecimals(actualValue, comparisonValue) <= 0;
        }

        return ((Comparable) actualValue).compareTo(comparisonValue) <= 0;
    }
}
