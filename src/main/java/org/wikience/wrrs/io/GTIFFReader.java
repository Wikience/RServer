package org.wikience.wrrs.io;

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

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderContext;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * (c) Antonio Rodriges, rodriges@wikience.org
 */
public class GTIFFReader {

    private final String path;
    private final RProtocol.TLatLonBox latLonBox;
    public boolean TEST_MODE = false;
    private int y = 4000;
    private int x = 4000;

    private GridCoverage2D grid;
    private Rectangle rectangle;
    private Raster inputRaster;
    private BufferedImage outputImage;

    private long fileReadMs = -1;

    public GTIFFReader(String path, RProtocol.TLatLonBox latLonBox) {
        this.path = path;
        this.latLonBox = latLonBox;
    }

    public Raster getInputRaster() {
        return inputRaster;
    }

    public long getFileReadMs() {
        return fileReadMs;
    }

    public void read() {
        long start = System.currentTimeMillis();
        readGrid();

        inputRaster = grid.getRenderedImage().getData(rectangle);

        long end = System.currentTimeMillis();
        fileReadMs = end - start;
    }

    public Future readInThread(ExecutorService executorService, CountDownLatch latch) {
        Runnable runnable = () -> {read(); latch.countDown();};
        Thread thread = new Thread(runnable);
        return executorService.submit(thread);
    }

    private void readGrid() {
        try {
            GeoTiffReader reader = new GeoTiffReader(new File(path));
            grid = reader.read(null);
            GridGeometry2D gg = grid.getGridGeometry();
            CoordinateReferenceSystem crs = CRS.decode("EPSG:4326");
            DirectPosition2D posWorld = new DirectPosition2D(crs, latLonBox.getLatitudeNorth(), latLonBox.getLongitudeWest());
            GridCoordinates2D posGrid = gg.worldToGrid(posWorld);
            x = posGrid.getCoordinateValue(0);
            y = posGrid.getCoordinateValue(1);
            rectangle = new Rectangle(x, y, latLonBox.getTileLonSize(), latLonBox.getTileLatSize());
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

    public RenderedImage readRenderedImage() {
        long start = System.currentTimeMillis();
        readGrid();

        RenderContext context = new RenderContext(new AffineTransform(), rectangle, new RenderingHints(Collections.emptyMap()));
        RenderedImage renderableImage = grid.getRenderableImage(0, 1).createRendering(context);

        long end = System.currentTimeMillis();
        fileReadMs = end - start;

        return renderableImage;
    }
}
