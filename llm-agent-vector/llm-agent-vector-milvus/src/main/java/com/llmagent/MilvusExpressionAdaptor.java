/*
 *  Copyright (c) 2023-2025, llm-agent (emirate.yang@gmail.com).
 *  <p>
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  <p>
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  <p>
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.llmagent;

import com.llmagent.vector.store.condition.Condition;
import com.llmagent.vector.store.condition.ExpressionAdaptor;
import com.llmagent.vector.store.condition.Operator;
import com.llmagent.vector.store.condition.Value;

import java.util.StringJoiner;

public class MilvusExpressionAdaptor implements ExpressionAdaptor {

    public static final MilvusExpressionAdaptor DEFAULT = new MilvusExpressionAdaptor();

    @Override
    public String toOperationSymbol(Operator operator) {
        if (operator == Operator.EQ) {
            return " == ";
        }
        return operator.getDefaultSymbol();
    }

    @Override
    public String toCondition(Condition condition) {
        if (condition.getOperator() == Operator.BETWEEN) {
            Object[] values = (Object[]) ((Value) condition.getRight()).getValue();
            return "(" + toLeft(condition.getLeft())
                    + toOperationSymbol(Operator.GE)
                    + values[0] + " && "
                    + toLeft(condition.getLeft())
                    + toOperationSymbol(Operator.LE)
                    + values[1] + ")";
        }

        return ExpressionAdaptor.super.toCondition(condition);
    }

    @Override
    public String toValue(Condition condition, Object value) {
        if (condition.getOperator() == Operator.IN) {
            Object[] values = (Object[]) value;
            StringJoiner stringJoiner = new StringJoiner(",", "[", "]");
            for (Object v : values) {
                if (v != null) {
                    stringJoiner.add("\"" + v + "\"");
                }
            }
            return stringJoiner.toString();
        }

        return ExpressionAdaptor.super.toValue(condition, value);
    }
}
