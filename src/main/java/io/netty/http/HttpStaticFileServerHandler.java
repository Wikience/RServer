/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.http;

import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.stream.ChunkedStream;
import io.netty.util.CharsetUtil;
import org.wikience.wrrs.compute.NDVIDemo;
import org.wikience.wrrs.io.Colorbar;
import org.wikience.wrrs.io.GTIFFReader;
import org.wikience.wrrs.io.RasterCompressor;
import org.wikience.wrrs.wrrsprotobuf.RProtocol;

import javax.activation.MimetypesFileTypeMap;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

import static io.netty.handler.codec.http.HttpHeaders.Names.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * A simple handler that serves incoming HTTP requests to send their respective
 * HTTP responses.  It also implements {@code 'If-Modified-Since'} header to
 * take advantage of browser cache, as described in
 * <a href="http://tools.ietf.org/html/rfc2616#section-14.25">RFC 2616</a>.
 * <p>
 * <h3>How Browser Caching Works</h3>
 * <p>
 * Web browser caching works with HTTP headers as illustrated by the following
 * sample:
 * <ol>
 * <li>Request #1 returns the content of {@code /file1.txt}.</li>
 * <li>Contents of {@code /file1.txt} is cached by the browser.</li>
 * <li>Request #2 for {@code /file1.txt} does return the contents of the
 * file again. Rather, a 304 Not Modified is returned. This tells the
 * browser to use the contents stored in its cache.</li>
 * <li>The server knows the file has not been modified because the
 * {@code If-Modified-Since} date is the same as the file's last
 * modified date.</li>
 * </ol>
 * <p>
 * <pre>
 * Request #1 Headers
 * ===================
 * GET /file1.txt HTTP/1.1
 *
 * Response #1 Headers
 * ===================
 * HTTP/1.1 200 OK
 * Date:               Tue, 01 Mar 2011 22:44:26 GMT
 * Last-Modified:      Wed, 30 Jun 2010 21:36:48 GMT
 * Expires:            Tue, 01 Mar 2012 22:44:26 GMT
 * Cache-Control:      private, max-age=31536000
 *
 * Request #2 Headers
 * ===================
 * GET /file1.txt HTTP/1.1
 * If-Modified-Since:  Wed, 30 Jun 2010 21:36:48 GMT
 *
 * Response #2 Headers
 * ===================
 * HTTP/1.1 304 Not Modified
 * Date:               Tue, 01 Mar 2011 22:44:28 GMT
 *
 * </pre>
 */
public class HttpStaticFileServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    public static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";
    public static final String HTTP_DATE_GMT_TIMEZONE = "GMT";
    public static final int HTTP_CACHE_SECONDS = 60;
    private static final Pattern INSECURE_URI = Pattern.compile(".*[<>&\"].*");
    private static final Pattern ALLOWED_FILE_NAME = Pattern.compile("[A-Za-z0-9][-_A-Za-z0-9\\.]*");
    private static ExecutorService executorService = Executors.newFixedThreadPool(2);
    public final String FILE_PATH = "d:\\RS_DATA\\Landsat\\8\\L1\\sr\\_\\179\\021\\LC81790212015146-SC20150806075046\\LC81790212015146LGN00_sr_band1.tif";
    public final String BAND05_NIR = "d:\\RS_DATA\\Landsat\\8\\L1\\sr\\_\\179\\021\\LC81790212015146-SC20150806075046\\LC81790212015146LGN00_sr_band5.tif";
    public final String BAND04_RED = "d:\\RS_DATA\\Landsat\\8\\L1\\sr\\_\\179\\021\\LC81790212015146-SC20150806075046\\LC81790212015146LGN00_sr_band4.tif";

    private static void sendRedirect(ChannelHandlerContext ctx, String newUri) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, FOUND);
        response.headers().set(LOCATION, newUri);

        // Close the connection as soon as the error message is sent.
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    private static void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HTTP_1_1, status, Unpooled.copiedBuffer("Failure: " + status + "\r\n", CharsetUtil.UTF_8));
        response.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8");

        // Close the connection as soon as the error message is sent.
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    /**
     * When file timestamp is the same as what the browser is sending up, send a "304 Not Modified"
     *
     * @param ctx Context
     */
    private static void sendNotModified(ChannelHandlerContext ctx) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, NOT_MODIFIED);
        setDateHeader(response);

        // Close the connection as soon as the error message is sent.
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    /**
     * Sets the Date header for the HTTP response
     *
     * @param response HTTP response
     */
    private static void setDateHeader(FullHttpResponse response) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
        dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));

        Calendar time = new GregorianCalendar();
        response.headers().set(DATE, dateFormatter.format(time.getTime()));
    }

    /**
     * Sets the Date and Cache headers for the HTTP Response
     *
     * @param response    HTTP response
     * @param fileToCache file to extract content type
     */
    private static void setDateAndCacheHeaders(HttpResponse response, File fileToCache) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
        dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));

        // Date header
        Calendar time = new GregorianCalendar();
        response.headers().set(DATE, dateFormatter.format(time.getTime()));

        // Add cache headers
        time.add(Calendar.SECOND, HTTP_CACHE_SECONDS);
        response.headers().set(EXPIRES, dateFormatter.format(time.getTime()));
        response.headers().set(CACHE_CONTROL, "private, max-age=" + HTTP_CACHE_SECONDS);
        response.headers().set(LAST_MODIFIED, dateFormatter.format(new Date(fileToCache.lastModified())));
    }

    /**
     * Sets the content type header for the HTTP Response
     *
     * @param response HTTP response
     */
    private static void setContentTypeHeader(HttpResponse response, String ext) {
        MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();
        response.headers().set(CONTENT_TYPE, mimeTypesMap.getContentType("a." + ext));
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        String coverage = null;
        int tileLatSize = 256;
        int tileLonSize = 256;
        double latN = 55.4;
        double lonW = 37.1;

        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.getUri());
        Map<String, List<String>> params = queryStringDecoder.parameters();
        if (!params.isEmpty()) {
            for (Map.Entry<String, List<String>> p : params.entrySet()) {
                if (p.getKey().equals("coverage")) {
                    coverage = p.getValue().get(0);
                }
                if (p.getKey().equals("tileLatSize")) {
                    tileLatSize = Integer.parseInt(p.getValue().get(0));
                }
                if (p.getKey().equals("tileLonSize")) {
                    tileLonSize = Integer.parseInt(p.getValue().get(0));
                }
                if (p.getKey().equalsIgnoreCase("latN")) {
                    latN = Double.parseDouble(p.getValue().get(0));
                }
                if (p.getKey().equalsIgnoreCase("lonW")) {
                    lonW = Double.parseDouble(p.getValue().get(0));
                }
            }
        }

        if (!request.getDecoderResult().isSuccess()) {
            sendError(ctx, BAD_REQUEST);
            return;
        }

        RProtocol.TLatLonBox.Builder builder = RProtocol.TLatLonBox.newBuilder();
        builder.setLatitudeNorth(latN).setLongitudeWest(lonW);
        builder.setTileLonSize(tileLonSize).setTileLatSize(tileLatSize);

        byte[] png = {};
        if (null == coverage) {
            GTIFFReader reader = new GTIFFReader(FILE_PATH, builder.build());
            reader.read();

            RasterCompressor compressor = new RasterCompressor();
            Colorbar colorbar = new Colorbar(Colorbar.HEAT_PALETTE, Colorbar.SR_INDEX);
            BufferedImage bufferedImage = compressor.toRGB(reader.getInputRaster(), colorbar);
            png = compressor.toPNG(bufferedImage);

            System.err.println("[\"readInputRaster\",\"toRGB\",\"toPNG\",\"size\"]");
            System.err.println(String.format("[%d,%d,%d,%d]",
                    reader.getFileReadMs(), compressor.getToRGBMs(), compressor.getToPNGMs(), png.length));
        } else {
            GTIFFReader readerNIR = new GTIFFReader(BAND05_NIR, builder.build());
            GTIFFReader readerRED = new GTIFFReader(BAND04_RED, builder.build());

            // Parallel reads
            CountDownLatch latch = new CountDownLatch(2);
            Future nir_t = readerNIR.readInThread(executorService, latch);
            Future red_t = readerRED.readInThread(executorService, latch);

            latch.await();

            Raster nir_r = readerNIR.getInputRaster();
            Raster red_r = readerRED.getInputRaster();

            NDVIDemo ndvi = new NDVIDemo();
            float[] result = ndvi.compute(nir_r.getDataBuffer(), red_r.getDataBuffer());

            RasterCompressor compressor = new RasterCompressor();
            Colorbar colorbar = new Colorbar(Colorbar.NDVI_HEAT_PALETTE, Colorbar.NDVI_INDEX);
            BufferedImage bufferedImage = compressor.toRGB(result, nir_r, colorbar);
            png = compressor.toPNG(bufferedImage);


            System.err.println("[\"readInputRaster\",\"toRGB\",\"toPNG\",\"size\",\"compute\"]");
            System.err.println(String.format("[%d,%d,%d,%d,%d]",
                    readerNIR.getFileReadMs() + readerRED.getFileReadMs(),
                    compressor.getToRGBMs(), compressor.getToPNGMs(), png.length,
                    ndvi.getComputeTime()));
        }


        ChunkedStream stream = new ChunkedStream(new ByteArrayInputStream(png));

//      sendError(ctx, NOT_FOUND);
        long tileSize = png.length;

        HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
        HttpHeaders.setContentLength(response, tileSize);
        response.headers().set(CONTENT_TYPE, "image/png");
//        setDateAndCacheHeaders(response, file);
        if (HttpHeaders.isKeepAlive(request)) {
            response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
        }

        response.headers().set(ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        response.headers().set(ACCESS_CONTROL_ALLOW_HEADERS, "x-requested-with");
        response.headers().set(CACHE_CONTROL, "no-cache, no-store, must-revalidate");
        response.headers().set(PRAGMA, "no-cache");
        response.headers().set(EXPIRES, "0");

        // Write the initial line and the header.
        ctx.write(response);

        // Write the content.
        ChannelFuture sendFileFuture;
        ChannelFuture lastContentFuture;
        sendFileFuture = ctx.writeAndFlush(new HttpChunkedInput(stream), ctx.newProgressivePromise());
        lastContentFuture = sendFileFuture;

        sendFileFuture.addListener(new ChannelProgressiveFutureListener() {
            @Override
            public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) {
//                if (total < 0) { // total unknown
//                    System.err.println(future.channel() + " Transfer progress: " + progress);
//                } else {
//                    System.err.println(future.channel() + " Transfer progress: " + progress + " / " + total);
//                }
            }

            @Override
            public void operationComplete(ChannelProgressiveFuture future) {
                System.err.println(future.channel() + " Transfer complete.");
            }
        });

        // Decide whether to close the connection or not.
        if (!HttpHeaders.isKeepAlive(request)) {
            // Close the connection when the whole content is written out.
            lastContentFuture.addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        if (ctx.channel().isActive()) {
            sendError(ctx, INTERNAL_SERVER_ERROR);
        }
    }
}
