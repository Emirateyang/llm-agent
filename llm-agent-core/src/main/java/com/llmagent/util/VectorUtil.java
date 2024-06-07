package com.llmagent.util;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class VectorUtil {

    private VectorUtil() {}

    public static float[] toFloatArray(List<Double> vector) {
        if (vector == null) {
            return null;
        }
        float[] output = new float[vector.size()];
        for (int i = 0; i < vector.size(); i++) {
            output[i] = vector.get(i).floatValue();
        }
        return output;
    }


    public static List<Float> toFloatList(List<Double> vector) {
        if (vector == null) {
            return null;
        }
        List<Float> output = new ArrayList<>(vector.size());
        for (double v : vector) {
            output.add((float) v);
        }
        return output;
    }

    public static List<Double> convertToVector(List<Float> floats) {
        if (floats == null) {
            return null;
        }
        return floats.stream().map(Float::doubleValue).collect(Collectors.toList());
    }

    public static List<Double> convertToVector(float[] floats) {
        if (floats == null) {
            return null;
        }
        List<Double> vector = new ArrayList<>(floats.length);
        for (int i = 0; i < floats.length; i++) {
            vector.add(Double.parseDouble(String.valueOf(floats[i])));
        }
        return vector;
    }
}
