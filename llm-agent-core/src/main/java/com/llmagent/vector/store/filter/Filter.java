package com.llmagent.vector.store.filter;

import com.llmagent.vector.store.filter.logical.And;
import com.llmagent.vector.store.filter.logical.Not;
import com.llmagent.vector.store.filter.logical.Or;

public interface Filter {

    /**
     * Tests if a given object satisfies this {@link Filter}.
     *
     * @param object An object to test.
     * @return {@code true} if a given object satisfies this {@link Filter}, {@code false} otherwise.
     */
    boolean test(Object object);

    default Filter and(Filter filter) {
        return and(this, filter);
    }

    static Filter and(Filter left, Filter right) {
        return new And(left, right);
    }

    default Filter or(Filter filter) {
        return or(this, filter);
    }

    static Filter or(Filter left, Filter right) {
        return new Or(left, right);
    }

    static Filter not(Filter expression) {
        return new Not(expression);
    }
}
