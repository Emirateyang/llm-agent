package com.llmagent.vector.store.filter.logical;

import com.llmagent.util.ValidationUtil;
import com.llmagent.vector.store.filter.Filter;

public class And implements Filter {

    private final Filter left;
    private final Filter right;

    public And(Filter left, Filter right) {
        this.left = ValidationUtil.ensureNotNull(left, "left");
        this.right = ValidationUtil.ensureNotNull(right, "right");
    }

    public Filter left() {
        return left;
    }

    public Filter right() {
        return right;
    }

    @Override
    public boolean test(Object object) {
        return left().test(object) && right().test(object);
    }
}
