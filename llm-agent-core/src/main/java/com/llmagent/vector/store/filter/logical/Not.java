package com.llmagent.vector.store.filter.logical;

import com.llmagent.util.ValidationUtil;
import com.llmagent.vector.store.filter.Filter;

public class Not implements Filter {

    private final Filter expression;

    public Not(Filter expression) {
        this.expression = ValidationUtil.ensureNotNull(expression, "expression");
    }

    public Filter expression() {
        return expression;
    }

    @Override
    public boolean test(Object object) {
        return !expression.test(object);
    }
}
