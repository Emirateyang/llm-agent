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

public class Key implements Expression {

    private Object key;

    public Key(Object key) {
        this.key = key;
    }

    public Object getKey() {
        return key;
    }

    public void setKey(Object key) {
        this.key = key;
    }

    @Override
    public String toExpression(ExpressionAdaptor adaptor) {
        return key != null ? key.toString() : "";
    }
}
