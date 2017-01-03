package org.wikience.wrrs.io;

import org.wikience.wrrs.wrrsprotobuf.RProtocol;

/**
 * (c) Antonio Rodriges, rodriges@wikience.org
 */
class GTIFFReaderTest {
    public final String FILE_PATH = "d:\\RS_DATA\\Landsat\\8\\L1\\sr\\_\\179\\021\\LC81790212015146-SC20150806075046\\LC81790212015146LGN00_sr_band3.tif";

    @org.junit.jupiter.api.Test
    void asPNG() {
        RProtocol.TLatLonBox.Builder builder = RProtocol.TLatLonBox.newBuilder();
        builder.setLatitudeNorth(55).setLongitudeWest(37);
        GTIFFReader reader = new GTIFFReader(FILE_PATH, builder.build());
        reader.TEST_MODE = true;
//        reader.asPNG();
//        reader.asGZIP();
    }
}