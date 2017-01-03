package org.wikience.wrrs.io;

/**
 * <p>
 * <p>
 * <p>
 * <br> (c) Antonio Rodriges, rodriges@wikience.org
 * <br> Started: 1/3/2017 12:33 PM
 */


public class Colorbar {

    public static final int HEAT_PALETTE[][] = {
            {0, 0, 0},
            {255, 255, 212},
            {254, 247, 197},
            {254, 239, 182},
            {254, 231, 167},
            {254, 223, 153},
            {254, 213, 136},
            {254, 200, 115},
            {254, 186, 94},
            {254, 173, 72},
            {254, 159, 51},
            {250, 146, 38},
            {242, 134, 32},
            {234, 122, 26},
            {226, 110, 21},
            {218, 98, 15},
            {206, 88, 12},
            {193, 79, 10},
            {179, 70, 8},
            {166, 61, 6},
            {153, 52, 4}
    };

    public static final double SR_INDEX[] = {
            0,
            98.825,
            127.263,
            155.7,
            184.138,
            212.576,
            241.013,
            269.451,
            297.889,
            326.326,
            354.764,
            383.202,
            411.64,
            440.077,
            468.515,
            496.953,
            525.39,
            553.828,
            582.266,
            610.703,
            639.141
    };

    public static final int NDVI_HEAT_PALETTE[][] = {
            {255, 255, 212},
            {254, 247, 197},
            {254, 239, 182},
            {254, 231, 167},
            {254, 223, 153},
            {254, 213, 136},
            {254, 200, 115},
            {254, 186, 94},
            {254, 173, 72},
            {254, 159, 51},
            {250, 146, 38},
            {242, 134, 32},
            {234, 122, 26},
            {226, 110, 21},
            {218, 98, 15},
            {206, 88, 12},
            {193, 79, 10},
            {179, 70, 8},
            {166, 61, 6},
            {153, 52, 4}
    };

    public static final double NDVI_INDEX[] = {
            -0.001277,
            0.044928,
            0.091133,
            0.137338,
            0.183543,
            0.229749,
            0.275954,
            0.322159,
            0.368364,
            0.414569,
            0.460774,
            0.506979,
            0.553184,
            0.599389,
            0.645594,
            0.6918,
            0.738005,
            0.78421,
            0.830415,
            0.87662
    };

    private int PALETTE[][];
    private double INDEX[];

    public Colorbar(int PALETTE[][], double[] INDEX) {
        this.PALETTE = PALETTE;
        this.INDEX = INDEX;
    }

    public void setColor(float value, byte[] dst, int index) {
        int ii = INDEX.length - 1;
        for (int i = 0; i < INDEX.length; i++) {
            if (value < INDEX[i]) {
                ii = i;
                break;
            }
        }

        index *= 3;

        dst[index] = (byte) PALETTE[ii][0];
        dst[index + 1] = (byte) PALETTE[ii][1];
        dst[index + 2] = (byte) PALETTE[ii][2];
    }
}
