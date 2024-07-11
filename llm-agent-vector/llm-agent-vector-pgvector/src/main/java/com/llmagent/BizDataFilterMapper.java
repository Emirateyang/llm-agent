package com.llmagent;

import com.llmagent.vector.store.filter.Filter;
import com.llmagent.vector.store.filter.comparison.*;

import static java.lang.String.format;

public class BizDataFilterMapper {
    final String bizDataColumn;

    public BizDataFilterMapper(String bizDataColumn) {
        this.bizDataColumn = bizDataColumn;
    }

    public String map(Filter filter) {
        if (filter instanceof SqlExpression) {
            return mapExpression((SqlExpression) filter);
        } else {
            throw new UnsupportedOperationException("Unsupported filter type: " + filter.getClass().getName());
        }
    }


    private String mapExpression(SqlExpression sqlExpression) {
        return format("%s is not null and %s", sqlExpression.key(), sqlExpression.comparisonExpression());
    }
}
