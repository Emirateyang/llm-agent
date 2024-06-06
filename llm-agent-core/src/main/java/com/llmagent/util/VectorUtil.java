package com.llmagent.util;

import java.util.ArrayList;
import java.util.List;

public class VectorUtil {
    public static float[] toFloatArray(double[] vector) {
        if (vector == null) {
            return null;
        }
        float[] output = new float[vector.length];
        for (int i = 0; i < vector.length; i++) {
            output[i] = (float) vector[i];
        }
        return output;
    }


    public static List<Float> toFloatList(double[] vector) {
        if (vector == null) {
            return null;
        }
        List<Float> output = new ArrayList<>(vector.length);
        for (double v : vector) {
            output.add((float) v);
        }
        return output;
    }

    public static double[] convertToVector(List<Float> floats) {
        if (floats == null) {
            return null;
        }
        double[] output = new double[floats.size()];
        int index = 0;
        for (Float aFloat : floats) {
            output[index++] = aFloat;
        }
        return output;
    }
}
