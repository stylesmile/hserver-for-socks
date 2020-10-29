package com.sokcs.handler.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;

import java.util.Base64;

/**
 * @author hxm
 */
public class HttpProxyAuthorizationHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            FullHttpRequest request = (FullHttpRequest) msg;
            try {
                boolean ok = false;
                String s = request.headers().get("Proxy-Authorization");
                String auth;
                if (s != null) {
                    auth = s.substring(6, s.length());
                    String s1 = new String(Base64.getDecoder().decode(auth.getBytes()), "utf-8");
                    System.out.println(s1);
                    String[] split = s1.split(":");
                    String userName = split[0];
                    String password = split[1];
                    if ("aa".equals(userName) || "aa".equals(password)) {
                        ok = true;
                    }
                }
                if (!ok) {
                    ctx.channel().writeAndFlush(HttpResponse.PROXY_AUTHENTICATION_REQUIRED);
                } else {
                    ctx.pipeline().remove(this.getClass());
                    ctx.fireChannelRead(msg);
                }

            } catch (Exception e) {
                e.printStackTrace();
                ctx.channel().writeAndFlush(HttpResponse.PROXY_AUTHENTICATION_REQUIRED);
            }
        }
    }
}