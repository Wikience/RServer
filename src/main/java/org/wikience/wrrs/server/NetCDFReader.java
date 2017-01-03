package org.wikience.wrrs.server;

import org.wikience.wrrs.wrrsprotobuf.RProtocol;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.IOException;

public class NetCDFReader {
    private RProtocol.RasterResponse.Builder rrB;

    public NetCDFReader(RProtocol.RasterResponse.Builder rrB) {
        this.rrB = rrB;
    }

    public void getData(String filename, int time, String type) throws IOException {

        // Read file
        NetcdfFile dataFile = NetcdfFile.open(filename);

        // Read temperatures
        Variable VAR = dataFile.findVariable(type);
        int shape[] = VAR.getShape();
        int origin[] = new int[shape.length];

        for (int i = 0; i < shape.length - 2; i++) shape[i] = 1;
        int timeshp = shape[0];
        double step = 24.0 / timeshp;
        double his = time / step;
        timeshp = (int) Math.floor(his);
        origin[0] = timeshp;

        Array n2array = null;
        try {
            n2array = VAR.read(origin, shape);
        } catch (InvalidRangeException e) {
            e.printStackTrace();
        }

        n2array = n2array.reduce();
        float[][] arr = (float[][]) n2array.copyToNDJavaArray();

        // Get units
        String units = VAR.findAttribute("units").getStringValue();

        // Get missingValue
        float missingValue = VAR.findAttribute("missing_value").getNumericValue().floatValue();

        // Latitude array
        Variable lat = dataFile.findVariable("lat");
        double[] latitude = (double[]) lat.read().copyToNDJavaArray();

        // Longitude array
        Variable lon = dataFile.findVariable("lon");
        double[] longitude = (double[]) lon.read().copyToNDJavaArray();

        // Get latitude scale
        float scaleLat = dataFile.findGlobalAttribute("LatitudeResolution").getNumericValue().floatValue();

        // Get longitude scale
        float scaleLon = dataFile.findGlobalAttribute("LongitudeResolution").getNumericValue().floatValue();

        float minValue = Float.MAX_VALUE;
        float maxValue = Float.MIN_VALUE;

        // Fill protobuf object with 2d-temperature-array
        RProtocol.RasterData.Builder arrBuilder = RProtocol.RasterData.newBuilder();

        for (int i = 0; i < shape[shape.length - 2]; i++) {
            for (int j = 0; j < shape[shape.length - 1]; j++) {
                float temp = arr[i][j];
                arrBuilder.addData(temp);

                // Update min temp
                if (temp < minValue) {
                    minValue = temp;
                }

                // Update max temp
                if (temp > maxValue && temp < missingValue) {
                    maxValue = temp;
                }
            }
        }

        // Fill latitudes
        RProtocol.Dimension1D.Builder lats = RProtocol.Dimension1D.newBuilder();
        for (double l : latitude) {
            lats.addValues(l);
        }

        // Fill longitudes
        RProtocol.Dimension1D.Builder lons = RProtocol.Dimension1D.newBuilder();
        for (double l : longitude) {
            lons.addValues(l);
        }

        lats.setStep(scaleLat);
        lats.setIndex(0);
        lons.setStep(scaleLon);
        lons.setIndex(1);

        RProtocol.RasterDimensions.Builder rDim = RProtocol.RasterDimensions.newBuilder();
        rDim.setLat(lats);
        rDim.setLon(lons);

        RProtocol.RasterAttributes.Builder rAttr = RProtocol.RasterAttributes.newBuilder();
        rAttr.setMissingValue(missingValue);
        rAttr.setUnits(units);
        rAttr.setMaxValue(maxValue);
        rAttr.setMinValue(minValue);

        rrB.setRasterDimensions(rDim);
        rrB.setRasterAttributes(rAttr);
        rrB.setRasterData(arrBuilder);
        // TODO: readInputRaster data offset and scale factor
    }
}
