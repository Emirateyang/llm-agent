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
package com.llmagent.vector.store;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VectorData {
    private final float[] vector;
    private List<Float> embedding;

    /**
     * Creates a new Embedding.
     * @param vector the vector, takes ownership of the array.
     */
    public VectorData(float[] vector) {
        this.vector = vector;
        this.embedding = vectorAsList();
    }

    /**
     * Returns the vector.
     * @return the vector.
     */
    public float[] vector() {
        return vector;
    }

    /**
     * Returns a copy of the vector as a list.
     * @return the vector as a list.
     */
    public List<Float> vectorAsList() {
        List<Float> list = new ArrayList<>(vector.length);
        for (float f : vector) {
            list.add(f);
        }
        return list;
    }

    public List<Float> embedding() {
        return embedding;
    }

    /**
     * Normalize vector
     */
    public void normalize() {
        double norm = 0.0;
        for (float f : vector) {
            norm += f * f;
        }
        norm = Math.sqrt(norm);

        for (int i = 0; i < vector.length; i++) {
            vector[i] /= norm;
        }
    }

    /**
     * Returns the dimension of the vector.
     * @return the dimension of the vector.
     */
    public int dimension() {
        return vector.length;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VectorData that = (VectorData) o;
        return Arrays.equals(this.vector, that.vector);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(vector);
    }

    @Override
    public String toString() {
        return "Embedding {" +
                " vector = " + Arrays.toString(vector) +
                " }";
    }

    /**
     * Creates a new Embedding from the given vector.
     * @param vector the vector, takes ownership of the array.
     * @return the new Embedding.
     */
    public static VectorData from(float[] vector) {
        return new VectorData(vector);
    }

    /**
     * Creates a new Embedding from the given vector.
     * @param vector the vector.
     * @return the new Embedding.
     */
    public static VectorData from(List<Float> vector) {
        float[] array = new float[vector.size()];
        for (int i = 0; i < vector.size(); i++) {
            array[i] = vector.get(i);
        }
        return new VectorData(array);
    }
}
