package org.wikience.wrrs.server;

import org.wikience.wrrs.wrrsprotobuf.RProtocol;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class GetRaster {

    private final static String ROOT_FOLDER = "netcdf/";

    private RProtocol.RasterRequest request;
    private RProtocol.RasterResponse.Builder rrB;

    private int hour = 0;

    public GetRaster(RProtocol.RasterRequest request, RProtocol.RasterResponse.Builder rrB) {
        this.request = request;
        this.rrB = rrB;
    }

    public void process() throws IOException {
        String layerId = request.getRequestParams().getDatasetId();
        String type = layerId.split("\\.")[2];
        long datems = request.getRequestParams().getTimeInterval().getTimeStartMillis();

        String folder = getFolder(layerId);
        String stringDate = getDate(datems);
        String filename = getFileName(folder, stringDate);

        System.out.println("Layer id: " + layerId + ", datetime: " + stringDate);

        filename = folder + filename;

        System.out.println(filename);

        NetCDFReader rdr = new NetCDFReader(rrB);

        rdr.getData(filename, hour, type);
  }

    private String getFolder(String layerId) {
        String folder = ROOT_FOLDER;
        String[] splitted = layerId.split("\\.");

        for (String part : splitted) {
            folder += part + "/";
        }

        return folder;
    }

    private String getDate(long datems) {
        Calendar date = GregorianCalendar.getInstance();
        date.clear();
        date.setTimeInMillis(datems);
        int year = date.get(Calendar.YEAR);
        int month = date.get(Calendar.MONTH) + 1;
        int day = date.get(Calendar.DAY_OF_MONTH);

        hour = date.get(Calendar.HOUR_OF_DAY);

        String folder = "" + year;

        if (month < 10) {
            folder += "0";
        }

        folder += "" + month;

        if (day < 10) {
            folder += "0";
        }

        folder += "" + day;

        return folder;
    }

    private String getFileName(String folder, String date) {
        List<String> results = new ArrayList<String>();

        File[] files = new File(folder).listFiles();

        for (File file : files) {
            if (file.isFile()) {
                results.add(file.getName());
            }
        }

        for (String filename : results) {
            if (filename.matches("(.*?)" + date + "(.*?)")) {
                return filename;
            }
        }

        return null;
    }

}
