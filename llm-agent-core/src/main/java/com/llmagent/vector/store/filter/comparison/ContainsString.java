package com.llmagent.vector.store.filter.comparison;

import com.llmagent.data.Metadata;
import com.llmagent.util.ValidationUtil;
import com.llmagent.vector.store.filter.Filter;
import lombok.EqualsAndHashCode;

import static com.llmagent.exception.Exceptions.illegalArgument;

@EqualsAndHashCode
public class ContainsString implements Filter {

    private final String key;
    private final String comparisonValue;

    public ContainsString(String key, String comparisonValue) {
            this.key = ValidationUtil.ensureNotBlank(key, "key");
            this.comparisonValue = ValidationUtil.ensureNotNull(comparisonValue, "comparisonValue with key '" + key + "'");
    }

    public String key() {
        return key;
    }

    public String comparisonValue() {
        return comparisonValue;
    }

    @Override
    public boolean test(Object object) {

        if (!(object instanceof Metadata metadata)) {
            return false;
        }

        if (!metadata.containsKey(key)) {
            return false;
        }

        Object actualValue = metadata.toMap().get(key);

        if (actualValue instanceof String str) {
            return str.contains(comparisonValue);
        }
        throw illegalArgument(
                "Type mismatch: actual value of metadata key \"%s\" (%s) has type %s, "
                        + "while it is expected to be a string",
                key, actualValue, actualValue.getClass().getName());
    }

    @Override
    public String toString() {
        return "ContainsString(key=" + this.key + ", comparisonValue=" + this.comparisonValue + ")";
    }
}
