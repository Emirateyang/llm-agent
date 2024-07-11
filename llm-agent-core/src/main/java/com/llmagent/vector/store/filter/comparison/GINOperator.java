package com.llmagent.vector.store.filter.comparison;

import com.llmagent.util.ValidationUtil;
import com.llmagent.vector.store.filter.Filter;
import lombok.EqualsAndHashCode;


@EqualsAndHashCode
public class GINOperator implements Filter {
    private final String key;
    private final String comparisonExpression;

    public GINOperator(String key, String comparisonExpression) {
        this.key = ValidationUtil.ensureNotBlank(key, "key");
        this.comparisonExpression = ValidationUtil.ensureNotNull(comparisonExpression, "comparisonExpression with key '" + key + "'");
    }

    public String key() {
        return key;
    }

    public String comparisonExpression() {
        return comparisonExpression;
    }

    @Override
    public boolean test(Object object) {
//        if (!(object instanceof Metadata)) {
//            return false;
//        }
//
//        Metadata metadata = (Metadata) object;
//        if (!metadata.containsKey(key)) {
//            return false;
//        }
//
//        Object actualValue = metadata.toMap().get(key);
//        ensureTypesAreCompatible(actualValue, comparisonValue, key);
//
//        if (actualValue instanceof Number) {
//            return NumberComparator.compareAsBigDecimals(actualValue, comparisonValue) == 0;
//        }
//
//        if (actualValue instanceof String) {
//            return actualValue.equals(comparisonValue.toString());
//        }
//
//        return actualValue.equals(comparisonValue);
        return true;
    }
}
