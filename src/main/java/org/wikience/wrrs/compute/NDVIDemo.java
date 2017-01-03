package org.wikience.wrrs.compute;

import it.geosolutions.jaiext.JAIExt;

import java.awt.image.DataBuffer;

/**
 * Created by Antonio Rodriges, rodriges@wikience.org on 10/15/2015.
 * Modified on 02 Jan 2017
 */
public class NDVIDemo {
    // Register JAI-ext operations
    static {
        JAIExt.initJAIEXT();
    }

    private long tCompute = -1;

    public long getComputeTime() {
        return tCompute;
    }

    public float[] compute(DataBuffer NIR, DataBuffer RED) {
        long start = System.currentTimeMillis();
        float[] result = new float[NIR.getSize()];

        float min = Float.MAX_VALUE, max = Float.MIN_VALUE;

        for (int i = 0; i < NIR.getSize(); i++) {
            result[i] = (float) ((NIR.getElem(i) - RED.getElem(i)) * 1.0) / (NIR.getElem(i) + RED.getElem(i));

//            if (result[i] < min) {
//                min = result[i];
//            }
//            if (result[i] > max) {
//                max = result[i];
//            }
        }

//        System.err.println(String.format("Max: %.2f, Min: %.2f", max, min));

        long end = System.currentTimeMillis();
        tCompute = end - start;

        return result;
    }
}
