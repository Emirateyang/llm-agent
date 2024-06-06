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
package com.llmagent.vector.store.condition;

public class Condition implements Expression {
    protected Operator operator;
    protected Expression left;
    protected Expression right;
    protected boolean effective = true;
    protected Connector connector;
    protected Condition prev;
    protected Condition next;

    public Condition() {
    }

    public Condition(Operator operator, Expression left, Expression right) {
        this.operator = operator;
        this.left = left;
        this.right = right;

        if (left instanceof Value) {
            ((Value) left).setCondition(this);
        }
        if (right instanceof Value) {
            ((Value) right).setCondition(this);
        }
    }


    public void connect(Condition nextCondition, Connector connector) {
        if (this.next != null) {
            this.next.connect(nextCondition, connector);
        } else {
            nextCondition.connector = connector;
            this.next = nextCondition;
            nextCondition.prev = this;
        }
    }

    public boolean checkEffective() {
        return effective;
    }

    protected Condition getPrevEffectiveCondition() {
        if (prev == null) {
            return null;
        }
        return prev.checkEffective() ? prev : prev.getPrevEffectiveCondition();
    }

    protected Condition getNextEffectiveCondition() {
        if (next == null) {
            return null;
        }
        return next.checkEffective() ? next : next.getNextEffectiveCondition();
    }


    @Override
    public String toExpression(ExpressionAdaptor adaptor) {
        StringBuilder expr = new StringBuilder();
        if (checkEffective()) {
            Condition prevEffectiveCondition = getPrevEffectiveCondition();
            if (prevEffectiveCondition != null && this.connector != null) {
                expr.append(adaptor.toConnector(this.connector));
            }
            expr.append(adaptor.toCondition(this));
        }

        if (this.next != null) {
            expr.append(this.next.toExpression(adaptor));
        }

        return expr.toString();
    }


    public Operator getOperator() {
        return operator;
    }

    public void setOperator(Operator operator) {
        this.operator = operator;
    }

    public Expression getLeft() {
        return left;
    }

    public void setLeft(Expression left) {
        this.left = left;
    }

    public Expression getRight() {
        return right;
    }

    public void setRight(Expression right) {
        this.right = right;
    }

    public boolean isEffective() {
        return effective;
    }

    public void setEffective(boolean effective) {
        this.effective = effective;
    }

    public Connector getConnector() {
        return connector;
    }

    public void setConnector(Connector connector) {
        this.connector = connector;
    }

    public Condition getPrev() {
        return prev;
    }

    public void setPrev(Condition prev) {
        this.prev = prev;
    }

    public Condition getNext() {
        return next;
    }

    public void setNext(Condition next) {
        this.next = next;
    }

}
