package org.wikience.wrrs.server;

import com.google.protobuf.InvalidProtocolBufferException;
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
import org.wikience.wrrs.wrrsprotobuf.RProtocol;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * (c) Antonio Rodriges, rodriges@wikience.org
 */
public class MessageHandler  {
    // testing purposes
    private static int TIMOUT_VERSION = 2000;
    private static int CORRUPT_BSON_VERSION = 3000;

    private final AttributeKey<Boolean> userConnected = AttributeKey.valueOf("uconnected");

    private byte[] rawMsg;
    private ChannelHandlerContext ctx;

    private byte[] response = getErrorBlob() ;

    public MessageHandler(byte[] rawMsg, ChannelHandlerContext ctx) {
        this.rawMsg = rawMsg;
        this.ctx = ctx;
    }

    public void handle() throws InvalidProtocolBufferException {
        // Check first stage of the protocol,
        // whether we are connected and versions coincide
        Attribute<Boolean> attr = ctx.attr(userConnected);
        Boolean isConnected = attr.get();

        if ( null == isConnected ) {
            // First message should be ConnectRequest message
            serveConnectRequest(attr);
        } else {
            // Every other message is RasterRequest
            serveRasterRequest();
        }

        sendMessage(response);
    }

    private void serveRasterRequest() {
        RProtocol.RasterResponse.Builder rrB = RProtocol.RasterResponse.newBuilder();
        RProtocol.ResponseStatus.Builder statB = RProtocol.ResponseStatus.newBuilder();

        try {
            RProtocol.RasterRequest rasterRequest = RProtocol.RasterRequest.parseFrom(rawMsg);
            rrB.setRequestResponseMeta(rasterRequest.getRequestResponseMeta());
            GetRaster getter = new GetRaster(rasterRequest, rrB );
            getter.process();
            statB.setCode(0); // success

        } catch (Exception e) {
            e.printStackTrace();

            // we do not throw exception, we should return Failure status
            statB.setCode(1);
            statB.setMessage(e.getMessage());
        }

        rrB.setResponseStatus(statB.build());
        response = rrB.build().toByteArray();

        return;
    }

    public byte[] getErrorBlob() {
        return "$ERROR".getBytes();
    }

    public byte[] getSuccessBlob() {
        return "$SUCCESS".getBytes();
    }

    public void sendMessage(byte[] msg) {
        ctx.channel().writeAndFlush(
                new BinaryWebSocketFrame(Unpooled.wrappedBuffer(msg)));
    }

    public void serveConnectRequest(Attribute<Boolean> attr) throws InvalidProtocolBufferException {
        RProtocol.ConnectRequest connectReq = RProtocol.ConnectRequest.parseFrom(rawMsg);

        if (serveDebugConnect(connectReq)) {
            return;
        }

        if (connectReq.hasProtocolVersion() &&
                connectReq.getProtocolVersion() == WebSocketServer.serverVersion ) {
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
