package com.sokcs.handler.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

public class HttpResponse extends DefaultFullHttpResponse {

    private static final ByteBuf RESPONSE_BODY = Unpooled.copiedBuffer(Unpooled.EMPTY_BUFFER);
    // 200
    public static final DefaultFullHttpResponse OK = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);

    // 4XX
    public static final HttpResponse PROXY_AUTHENTICATION_REQUIRED = new HttpResponse(HttpResponseStatus.PROXY_AUTHENTICATION_REQUIRED);

    static {
        PROXY_AUTHENTICATION_REQUIRED.headers().set(HttpHeaderNames.PROXY_AUTHENTICATE, "Basic realm=\"fuck username and password\"");
        PROXY_AUTHENTICATION_REQUIRED.headers().set(HttpHeaderNames.CONNECTION, "keep-alive");
        OK.headers().set(HttpHeaderNames.CONNECTION, "keep-alive");
    }

    public HttpResponse(HttpResponseStatus responseStatus) {
        super(HttpVersion.HTTP_1_1, responseStatus, RESPONSE_BODY);
        headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=utf-8");
        headers().set(HttpHeaderNames.CONTENT_LENGTH, 0);
        headers().set(HttpHeaderNames.CONNECTION, "close");
    }
}