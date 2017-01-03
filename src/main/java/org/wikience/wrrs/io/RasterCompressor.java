package org.wikience.wrrs.io;

import com.jcraft.jzlib.Deflater;
import com.jcraft.jzlib.DeflaterOutputStream;
import com.jcraft.jzlib.JZlib;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * <p>
 * <p>
 * <p>
 * <br> (c) Antonio Rodriges, rodriges@wikience.org
 * <br> Started: 1/2/2017 6:43 PM
 */
public class RasterCompressor {

    public RasterCompressor(){}

    private boolean TEST_MODE = false;
    private String testFileName = "imageX";

    private long toZIPms = -1, toRGBMs = -1, toPNGMs = -1;

    public long getToZIPms() {
        return toZIPms;
    }

    public long getToRGBMs() {
        return toRGBMs;
    }

    public long getToPNGMs() {
        return toPNGMs;
    }

    public void setTEST_MODE(boolean mode) {
        this.TEST_MODE = mode;
    }

    public void setTestFile(String fileName) {
        this.testFileName = fileName;
    }

    public byte[] toPNG(BufferedImage outputImage) {
        long start = System.currentTimeMillis();

        ByteArrayOutputStream oStream = null;
        try {
            oStream = new ByteArrayOutputStream();
            ImageIO.write(outputImage, "png", oStream);
            if (TEST_MODE) {
                ImageIO.write(outputImage, "png", new File(testFileName + ".png"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        long end = System.currentTimeMillis();
        byte[] result = oStream.toByteArray();
        toPNGMs = end - start;

        return result;
    }

    public BufferedImage toRGB(Raster inputRaster, Colorbar colorbar) {
        long start = System.currentTimeMillis();

        BufferedImage outputImage;

        DataBuffer origImage = inputRaster.getDataBuffer();
        byte [] newByteArray = new byte[inputRaster.getWidth() * inputRaster.getHeight() * 3]; // 3 bytes: RGB

        for (int i = 0; i < origImage.getSize(); i++) {
            colorbar.setColor(origImage.getElem(i), newByteArray, i);
        }

        DataBuffer buffer = new DataBufferByte(newByteArray, newByteArray.length);

        //3 bytes per pixel: red, green, blue
        WritableRaster raster = Raster.createInterleavedRaster(
                buffer,
                inputRaster.getWidth(), inputRaster.getHeight(),
                3 * inputRaster.getWidth(),
                3,
                new int[]{0, 1, 2},
                (Point) null);

        ColorModel cm = new ComponentColorModel(ColorModel.getRGBdefault().getColorSpace(), false, true, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
        outputImage = new BufferedImage(cm, raster, true, null);

        long end = System.currentTimeMillis();
        toRGBMs = end - start;

        return outputImage;
    }

    public BufferedImage toRGB(float[] origImage, Raster inputRaster, Colorbar colorbar) {
        long start = System.currentTimeMillis();

        BufferedImage outputImage;

        byte [] newByteArray = new byte[inputRaster.getWidth() * inputRaster.getHeight() * 3]; // 3 bytes: RGB

        for (int i = 0; i < origImage.length; i++) {
            colorbar.setColor(origImage[i], newByteArray, i);
        }

        DataBuffer buffer = new DataBufferByte(newByteArray, newByteArray.length);

        //3 bytes per pixel: red, green, blue
        WritableRaster raster = Raster.createInterleavedRaster(
                buffer,
                inputRaster.getWidth(), inputRaster.getHeight(),
                3 * inputRaster.getWidth(),
                3,
                new int[]{0, 1, 2},
                (Point) null);

        ColorModel cm = new ComponentColorModel(ColorModel.getRGBdefault().getColorSpace(), false, true, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
        outputImage = new BufferedImage(cm, raster, true, null);

        long end = System.currentTimeMillis();
        toRGBMs = end - start;

        return outputImage;
    }

    public byte[] asGZIP(Raster inputRaster) {
        long start = System.currentTimeMillis();

        DataBuffer dataBuffer = inputRaster.getDataBuffer();

        ByteBuffer byteBuffer = ByteBuffer.allocate(dataBuffer.getSize()*2); // SHORT or int16
        for (int i = 0; i < dataBuffer.getSize(); i++) {
            short value = (short)dataBuffer.getElem(i);
            byteBuffer.put((byte)(value & 0xFF));
            byteBuffer.put((byte)((value >> 8) & 0xFF));
        }
        byte[] outArray = byteBuffer.array();

        DeflaterOutputStream deflaterOutputStream;
        ByteArrayOutputStream oStream = null;
        try {
            if (TEST_MODE) {
                Deflater deflater = new Deflater(JZlib.Z_BEST_COMPRESSION);
                File zlib_compressed = new File(testFileName + ".cmp");
                deflaterOutputStream = new DeflaterOutputStream(new FileOutputStream(zlib_compressed), deflater);
                deflaterOutputStream.write(outArray, 0, outArray.length);
                deflaterOutputStream.close();
            }
            oStream = new ByteArrayOutputStream();
            deflaterOutputStream = new DeflaterOutputStream(oStream);
            deflaterOutputStream.write(outArray, 0, outArray.length);
            deflaterOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        long end = System.currentTimeMillis();
        byte [] result = oStream.toByteArray();
        toZIPms = end - start;

        return result;
    }
}
