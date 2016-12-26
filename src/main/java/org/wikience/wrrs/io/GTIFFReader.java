package org.wikience.wrrs.io;

import com.jcraft.jzlib.Deflater;
import com.jcraft.jzlib.DeflaterOutputStream;
import com.jcraft.jzlib.JZlib;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.data.DataSourceException;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.wikience.wrrs.wrrsprotobuf.RProtocol;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * (c) Antonio Rodriges, rodriges@wikience.org
 */
public class GTIFFReader {
    public final int PALETTE[][] = {
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

    public final double COLOR_INDEX[] = {
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

    private final String path;
    private final RProtocol.TLatLonBox latLonBox;
    private int y = 4000;
    private int x = 4000;
    private int h = 512;
    private int w = 512;
    private Raster raster;
    private byte[] newByteArray;
    private BufferedImage outImage;

    public GTIFFReader(String path, RProtocol.TLatLonBox latLonBox) {
        this.path = path;
        this.latLonBox = latLonBox;
    }

    public byte[] asPNG() {
        read();
        toRGB();

        try {
            ImageIO.write(outImage, "png", new File("image2.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public void asGZIP() {
        read();
        toRGB();

        File zlib_compressed = new File("image2.cmp");
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
    }

    private void toRGB() {
        DataBuffer origImage = raster.getDataBuffer();
        newByteArray = new byte[w * h * 3]; // 3 bytes: RGB

        for (int i = 0; i < origImage.getSize(); i++) {
            setColor(origImage.getElem(i), newByteArray, i);
        }

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
        outImage = new BufferedImage(cm, raster, true, null);
    }

    private void read() {
        GridCoverage2D grid;
        try {
            GeoTiffReader reader = new GeoTiffReader(new File(path));
            grid = reader.read(null);
            GridGeometry2D gg = grid.getGridGeometry();
            CoordinateReferenceSystem crs = CRS.decode("EPSG:4326");
            DirectPosition2D posWorld = new DirectPosition2D(crs, latLonBox.getLatitudeNorth(), latLonBox.getLongitudeWest());
            GridCoordinates2D posGrid = gg.worldToGrid(posWorld);
            x = posGrid.getCoordinateValue(0);
            y = posGrid.getCoordinateValue(1);
            Rectangle rectangle = new Rectangle(x, y, 512, 512);
            raster = grid.getRenderedImage().getData(rectangle);
        } catch (DataSourceException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAuthorityCodeException e) {
            e.printStackTrace();
        } catch (FactoryException e) {
            e.printStackTrace();
        } catch (TransformException e) {
            e.printStackTrace();
        }
    }

    private void setColor(int value, byte[] dst, int index) {
        int ii = COLOR_INDEX.length - 1;
        for (int i = 0; i < COLOR_INDEX.length; i++) {
            if (value < COLOR_INDEX[i]) {
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