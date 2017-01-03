package org.wikience.wrrs;

import com.jcraft.jzlib.Deflater;
import com.jcraft.jzlib.DeflaterOutputStream;
import com.jcraft.jzlib.JZlib;
import it.geosolutions.jaiext.JAIExt;
import org.wikience.wrrs.io.Colorbar;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * <p>
 * <p>
 * <p>
 * <br> (c) Antonio Rodriges, rodriges@wikience.org
 * <br> Started: 12/25/2016 7:59 PM
 */
public class Evaluation {
    static {
        JAIExt.initJAIEXT();
    }

    public final String FILE_PATH = "d:\\RS_DATA\\Landsat\\8\\L1\\sr\\_\\179\\021\\LC81790212015146-SC20150806075046\\LC81790212015146LGN00_sr_band3.tif";

    public static void main(String args[]) {
        new Evaluation().evaluate();
    }

    public void evaluate() {
        BufferedImage image;
        try {
            image = ImageIO.read(new File(FILE_PATH));
        } catch (IOException e1) {
            e1.printStackTrace();
            return;
        }

        DataBuffer origImage = image.getRaster().getDataBuffer();

        int y = 4000;
        int h = 512;
        int x = 4000;
        int w = 512;
        int num = 0;
        int cnt = 0;

        byte[] newByteArray = new byte[w * h * 3]; // 3 bytes: RGB
        int idx_dst = 0;

        Colorbar colorbar = new Colorbar(Colorbar.HEAT_PALETTE, Colorbar.NDVI_INDEX);

        while (num < h) {
            int idx_src = image.getWidth() * (y + num) + x;

            for (int i = 0; i < w; i++) {
                int value = origImage.getElem(idx_src);
                colorbar.setColor(origImage.getElem(idx_src), newByteArray, idx_dst++);
                if (value == -9999) {
                    cnt++;
                }
                idx_src++;
            }
            num++;
        }

        System.err.println(cnt);

        File zlib_compressed = new File("image.cmp");
        DeflaterOutputStream deflaterOutputStream;
        try {
            Deflater deflater = new Deflater(JZlib.Z_BEST_COMPRESSION);
            deflaterOutputStream = new DeflaterOutputStream(new FileOutputStream(zlib_compressed), deflater);
//            deflaterOutputStream = new DeflaterOutputStream(new FileOutputStream(zlib_compressed));
            deflaterOutputStream.write(newByteArray, 0, newByteArray.length);
            deflaterOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


//        convert bufferedImage to double array
//        getSubimage work in an unanticipated way
//        image = image.getSubimage((int) (4150), (int) (7000), (int) (512), (int) (512));
//        int origSize = origImage.getSize();
//        byte[] newByteArray = new byte[origSize * 3]; // 3 bytes: RGB
//        for (int i = 0; i < origSize; i++) {
////            setColor(origImage.getElem(i), newByteArray, i);
//            if (origImage.getElem(i) == -9999) {
//                newByteArray[i * 3] = newByteArray[i * 3 + 1] = (byte)255;
//            } else {
//                newByteArray[i * 3] = newByteArray[i * 3 + 1] = 0;
//            }
//            newByteArray[i * 3 + 2] = (byte) (origImage.getElem(i) % 100);
//        }

        DataBuffer buffer = new DataBufferByte(newByteArray, newByteArray.length);

        //3 bytes per pixel: red, green, blue
        WritableRaster raster = Raster.createInterleavedRaster(
                buffer,
                w, h,
                3 * w,
                3,
                new int[]{0, 1, 2},
                (Point) null);

        ColorModel cm = new ComponentColorModel(ColorModel.getRGBdefault().getColorSpace(), false, true, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
        BufferedImage outImage = new BufferedImage(cm, raster, true, null);

        ByteArrayOutputStream baos = null;
        try {
            baos = new ByteArrayOutputStream();
//            ImageIO.write(outImage, "png", baos);
            ImageIO.write(outImage, "png", new File("image.png"));
            //baos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

//        byte[] barray = baos.toByteArray();
//
//        System.err.println(barray.length);
    }

}
