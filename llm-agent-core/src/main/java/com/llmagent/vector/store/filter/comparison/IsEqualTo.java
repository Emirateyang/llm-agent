package com.llmagent.vector.store.filter.comparison;

import com.llmagent.data.Metadata;
import com.llmagent.util.NumberComparator;
import com.llmagent.util.ValidationUtil;
import com.llmagent.vector.store.filter.Filter;

import static com.llmagent.vector.store.filter.comparison.TypeChecker.ensureTypesAreCompatible;

public class IsEqualTo implements Filter {
    private final String key;
    private final Object comparisonValue;

    public IsEqualTo(String key, Object comparisonValue) {
        this.key = ValidationUtil.ensureNotBlank(key, "key");
        this.comparisonValue = ValidationUtil.ensureNotNull(comparisonValue, "comparisonValue with key '" + key + "'");
    }

    public String key() {
        return key;
    }

    public Object comparisonValue() {
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
            return NumberComparator.compareAsBigDecimals(actualValue, comparisonValue) == 0;
        }

        return actualValue.equals(comparisonValue);
    }
}
