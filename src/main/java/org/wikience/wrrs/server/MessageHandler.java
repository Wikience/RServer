package org.wikience.wrrs.server;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.jcraft.jzlib.Deflater;
import com.jcraft.jzlib.DeflaterOutputStream;
import com.jcraft.jzlib.GZIPException;
import com.jcraft.jzlib.JZlib;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.bson.BsonBinaryWriter;
import org.bson.BsonDocument;
import org.bson.codecs.BsonDocumentCodec;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;
import org.bson.io.BasicOutputBuffer;
import org.json.XML;
import io.netty.websocket.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikience.wrrs.io.GTIFFReader;
import org.wikience.wrrs.wrrsprotobuf.RProtocol;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * (c) Antonio Rodriges, rodriges@wikience.org
 */
public class MessageHandler {
    private static final Logger LOG = LoggerFactory.getLogger(MessageHandler.class);

    // testing purposes
    private static final int TIMOUT_VERSION = 2000;
    private static final int CORRUPT_BSON_VERSION = 3000;
    private static final int COMPRESS_TEST = 4000;

    public final String FILE_PATH = "d:\\RS_DATA\\Landsat\\8\\L1\\sr\\_\\179\\021\\LC81790212015146-SC20150806075046\\LC81790212015146LGN00_sr_band3.tif";

    private final AttributeKey<Boolean> userConnected = AttributeKey.valueOf("uconnected");

    private byte[] rawMsg;
    private ChannelHandlerContext ctx;

    private byte[] response = getErrorBlob();

    public MessageHandler(byte[] rawMsg, ChannelHandlerContext ctx) {
        this.rawMsg = rawMsg;
        this.ctx = ctx;
    }

    public void handle() throws InvalidProtocolBufferException {
        // Check first stage of the protocol,
        // whether we are connected and versions coincide
        Attribute<Boolean> attr = ctx.attr(userConnected);
        Boolean isConnected = attr.get();

        if (null == isConnected) {
            // First message should be ConnectRequest message
            serveConnectRequest(attr);
        } else {
            // Every other message is RasterRequest
            serveRasterRequest();
        }

        sendMessage(response);
        LOG.debug("Message sent");
    }

    private RProtocol.RasterData.Builder compress(byte[] outArray, int bytesPerElement) throws IOException {
        RProtocol.RasterData.Builder arrBuilder = RProtocol.RasterData.newBuilder();

        arrBuilder.setBytesPerElement(bytesPerElement);
        arrBuilder.setCompressionMethod(RProtocol.RasterData.COMPRESSION_METHOD.ZLIB);

        ByteArrayOutputStream oStream = new ByteArrayOutputStream();

        Deflater deflater = new Deflater(JZlib.Z_BEST_COMPRESSION);
        DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(oStream, deflater);
        deflaterOutputStream.write(outArray, 0, outArray.length);
        deflaterOutputStream.close();
        ByteString bytes = ByteString.copyFrom(oStream.toByteArray());

        arrBuilder.setDataCompressed(bytes);

        return arrBuilder;
    }

    private void serveRasterRequest() {
        RProtocol.RasterResponse.Builder rrB = RProtocol.RasterResponse.newBuilder();
        RProtocol.ResponseStatus.Builder statB = RProtocol.ResponseStatus.newBuilder();

        try {
            RProtocol.RasterRequest rasterRequest = RProtocol.RasterRequest.parseFrom(rawMsg);
            rrB.setRequestResponseMeta(rasterRequest.getRequestResponseMeta());

            if (rasterRequest.getRequestResponseMeta().hasFlag()) {
                switch (rasterRequest.getRequestResponseMeta().getFlag()) {
                    case COMPRESS_TEST:
                        RProtocol.Dimension1D.Builder lons = RProtocol.Dimension1D.newBuilder();
                        lons.setStart(1).setEnd(3).setStep(1).setIndex(0);

                        RProtocol.RasterDimensions.Builder rDim = RProtocol.RasterDimensions.newBuilder();
                        rDim.setLon(lons);

                        RProtocol.RasterAttributes.Builder rAttr = RProtocol.RasterAttributes.newBuilder();
                        rAttr.setMissingValue(-9999);
                        rAttr.setUnits("B");
                        rAttr.setMaxValue(1000);
                        rAttr.setMinValue(0);

                        byte[] outArray = {1, 2, 3};
                        rrB.setRasterDimensions(rDim);
                        rrB.setRasterAttributes(rAttr);
                        rrB.setRasterData(compress(outArray, 1));
                        statB.setCode(0);

                        break;
                }
            } else {
                if (rasterRequest.getRequestParams().getDatasetId().startsWith("GTIFF")) {
                    RProtocol.TLatLonBox.Builder builder = RProtocol.TLatLonBox.newBuilder();
                    builder.setLatitudeNorth(55.4).setLongitudeWest(37.1);
                    if (rasterRequest.getRequestParams().hasLatLonBox()) {
                        RProtocol.TLatLonBox box = rasterRequest.getRequestParams().getLatLonBox();
                        builder.setTileLatSize(box.getTileLatSize()).setTileLonSize(box.getTileLonSize());
                    } else {
                        builder.setTileLonSize(256).setTileLatSize(256);
                    }

                    GTIFFReader reader = new GTIFFReader(FILE_PATH, builder.build());
                    byte[] result = reader.asGZIP();

                    RProtocol.Dimension1D.Builder lons = RProtocol.Dimension1D.newBuilder();
                    lons.setStart(1).setEnd(builder.getTileLonSize()).setStep(1).setIndex(0);

                    RProtocol.Dimension1D.Builder lats = RProtocol.Dimension1D.newBuilder();
                    lats.setStart(1).setEnd(builder.getTileLatSize()).setStep(1).setIndex(1);

                    RProtocol.RasterDimensions.Builder rDim = RProtocol.RasterDimensions.newBuilder();
                    rDim.setLat(lats);
                    rDim.setLon(lons);

                    RProtocol.RasterAttributes.Builder rAttr = RProtocol.RasterAttributes.newBuilder();
                    rAttr.setMissingValue(-9999);
                    rAttr.setUnits("B");
                    rAttr.setMaxValue(1000);
                    rAttr.setMinValue(0);

                    rrB.setRasterDimensions(rDim);
                    rrB.setRasterAttributes(rAttr);

                    RProtocol.RasterData.Builder arrBuilder = RProtocol.RasterData.newBuilder();

                    arrBuilder.setBytesPerElement(2);
                    arrBuilder.setCompressionMethod(RProtocol.RasterData.COMPRESSION_METHOD.ZLIB);
                    arrBuilder.setDataCompressed(ByteString.copyFrom(result));
                    rrB.setRasterData(arrBuilder);

                    rrB.setStatistics(reader.getStatistics());

                    statB.setCode(0); // success
                } else {
                    GetRaster getter = new GetRaster(rasterRequest, rrB);
                    getter.process();
                    statB.setCode(0); // success
                }
            }
        } catch (Exception e) {
            e.printStackTrace();

            // we do not throw exception, we should return Failure status
            statB.setCode(1);
            statB.setMessage(e.getMessage());
        }

        rrB.setResponseStatus(statB.build());
        response = rrB.build().toByteArray();
    }

    public byte[] getErrorBlob() {
        return "$ERROR".getBytes();
    }

    public byte[] getSuccessBlob() {
        return "$SUCCESS".getBytes();
    }

    public void sendMessage(byte[] msg) {
        ctx.writeAndFlush(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(msg)));
    }

    public void serveConnectRequest(Attribute<Boolean> attr) throws InvalidProtocolBufferException {
        RProtocol.ConnectRequest connectReq = RProtocol.ConnectRequest.parseFrom(rawMsg);

        if (serveDebugConnect(connectReq)) {
            return;
        }

        if (connectReq.hasProtocolVersion() &&
                connectReq.getProtocolVersion() == WebSocketServer.serverVersion) {
            attr.set(Boolean.TRUE); // next messages will be data requests

            if (connectReq.hasRetrieveDatasetTree() && connectReq.getRetrieveDatasetTree()) {
                response = getLayerList();
            } else {
                response = getSuccessBlob();
            }
        } else {
            response = getErrorBlob();
        }

    }

    // true = debug case
    public boolean serveDebugConnect(RProtocol.ConnectRequest connectReq) {
        if (connectReq.hasProtocolVersion() &&
                connectReq.getProtocolVersion() == TIMOUT_VERSION) {
            // I should time out
            try {
                Thread.sleep(5000);
                return true;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (connectReq.hasProtocolVersion() &&
                connectReq.getProtocolVersion() == CORRUPT_BSON_VERSION) {
            response = "$CORRUPTED BSON".getBytes();
            return true;
        }

        return false;
    }

    public byte[] getLayerList() {
        try {
            String layerList =
                    new String(Files.readAllBytes(Paths.get(
                            WebSocketServer.configuration.getValueOf("layerListFilePath"))));

            org.json.JSONObject jsonObject = XML.toJSONObject(layerList);
            BsonDocument doc = BsonDocument.parse(jsonObject.toString());
            BasicOutputBuffer outputBuffer = new BasicOutputBuffer();
            BsonBinaryWriter writer = new BsonBinaryWriter(outputBuffer);
            Codec<BsonDocument> documentCodec = new BsonDocumentCodec();
            documentCodec.encode(writer, doc, EncoderContext.builder().isEncodingCollectibleDocument(true).build());

            System.out.println("Get layer list");

            return outputBuffer.toByteArray();

        } catch (Exception e) {
            return getErrorBlob();
        }
    }
}
