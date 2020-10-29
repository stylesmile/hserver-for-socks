package com.sokcs.handler.http;

import com.sokcs.certificate.CertificatePool;
import com.sokcs.pojo.CertificateInfo;
import com.sokcs.utils.ChannelUtils;
import com.sokcs.utils.MsgUtils;
import com.sokcs.utils.ReleaseUtils;
import com.sokcs.utils.ThrowableUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.SSLException;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;


@Slf4j
public class HttpHandler extends SimpleChannelInboundHandler<HttpObject> {
    private ChannelHandlerContext clientContext;
    private final CertificatePool certificatePool;
    private final Consumer<FullHttpRequest> consumer;
    private final SslContext clientSslContext;
    private final Bootstrap bootstrap = new Bootstrap();

    public HttpHandler(CertificatePool certificatePool, Consumer<FullHttpRequest> consumer,
                       SslContext clientSslContext) {
        this.certificatePool = certificatePool;
        this.consumer = consumer;
        this.clientSslContext = clientSslContext;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        this.clientContext = ctx;
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ThrowableUtils.message(this.getClass(), cause);
        ctx.close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            FullHttpRequest fullHttpRequest = (FullHttpRequest) msg;

            int port = 80;

            String[] hostSplit = fullHttpRequest.headers().get(HttpHeaderNames.HOST).split(":");
            String host = hostSplit[0];
            if (hostSplit.length > 1) {
                port = Integer.parseInt(hostSplit[1]);
            }

            // http连接
            if (!fullHttpRequest.method().equals(HttpMethod.CONNECT)) {
                if (Objects.nonNull(consumer)) {
                    // 如果是http请求，无需解密，直接获取
                    consumer.accept(fullHttpRequest);
                }

                httpHandle(fullHttpRequest, host, port);
                return;
            }

            if (Objects.nonNull(consumer)) {
                httpsHandleCapture(sslContext(host, port), host, port);
            } else {
                httpsHandle(host, port);
            }
        }
    }

    /**
     * 直接转发http数据
     *
     * @param fullHttpRequest
     * @param host
     * @param port
     */
    private void httpHandle(FullHttpRequest fullHttpRequest, String host, int port) {
        fullHttpRequest.retain();
        Object object = MsgUtils.fromHttpRequest(fullHttpRequest);

        connect(host, port, new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(new ExchangeHandler(clientContext.channel()));
                clientContext.channel().closeFuture().addListener(future -> ChannelUtils.close(ch));
            }
        }).addListener((ChannelFutureListener) future -> {
            try {
                if (future.isSuccess()) {
                    ChannelPipeline channelPipeline = clientContext.pipeline();

                    removeHandler(channelPipeline, HandlerName.HTTP_HANDLER);
                    removeHandler(channelPipeline, HandlerName.HTTP_SERVER_CODEC);
                    removeHandler(channelPipeline, HandlerName.HTTP_OBJECT_AGGREGATOR);

                    Channel channel = future.channel();

                    channelPipeline.addLast(new ExchangeHandler(channel));
                    // 发送请求数据
                    channel.writeAndFlush(object);
                } else {
                    clientContext.close();
                }
            } finally {
                ReleaseUtils.release(object);
            }
        });
    }

    /**
     * 直接转发https数据
     *
     * @param host
     * @param port
     */
    private void httpsHandle(String host, int port) {
        connect(host, port, new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(new ExchangeHandler(clientContext.channel()));
                clientContext.channel().closeFuture().addListener(future -> ChannelUtils.close(ch));
            }
        }).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);

                clientContext.writeAndFlush(response).addListener((ChannelFutureListener) channelFuture -> {
                    ChannelPipeline channelPipeline = clientContext.pipeline();

                    removeHandler(channelPipeline, HandlerName.HTTP_HANDLER);
                    removeHandler(channelPipeline, HandlerName.HTTP_SERVER_CODEC);
                    removeHandler(channelPipeline, HandlerName.HTTP_OBJECT_AGGREGATOR);

                    channelPipeline.addLast(new ExchangeHandler(future.channel()));
                });
            } else {
                clientContext.close();
            }
        });
    }

    /**
     * 捕获https请求内容
     *
     * @param sslContext
     * @param host
     * @param port
     */
    private void httpsHandleCapture(SslContext sslContext, String host, int port) {
        connect(host, port, new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline channelPipeline = ch.pipeline();

                // 处理与目标服务器的ssl
                channelPipeline.addFirst(clientSslContext.newHandler(ch.alloc()));

                channelPipeline.addLast(new HttpClientCodec());
                channelPipeline.addLast(new HttpObjectAggregator(1024 * 1024 * 512));

                Channel channel = clientContext.channel();
                channel.closeFuture().addListener(future -> ChannelUtils.close(ch));

                channelPipeline.addLast(new CaptureExchangeHandler(channel, "远程服务器"));
            }
        }).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);

                clientContext.writeAndFlush(response).addListener((ChannelFutureListener) channelFuture -> {
                    ChannelPipeline channelPipeline = clientContext.pipeline();

                    removeHandler(channelPipeline, HandlerName.HTTP_HANDLER);

                    // 处理与客户端的ssl
                    channelPipeline.addFirst(sslContext.newHandler(clientContext.alloc()));
                    //前面还有 new HttpServerCodec()、new HttpObjectAggregator(1024 * 1024 * 512)
                    channelPipeline.addLast(new CaptureExchangeHandler(consumer, future.channel(), "客户端"));
                });
            } else {
                clientContext.close();
            }
        });
    }

    private SslContext sslContext(String host, int port) {
        try {
            CertificateInfo certificateInfo = certificatePool.getCertificateInfo(host, port);

            if (Objects.nonNull(certificateInfo)) {
                return SslContextBuilder.forServer(certificateInfo.getKeyPair().getPrivate(),
                        certificateInfo.getX509Certificate()).build();
            }
        } catch (SSLException e) {
            log.error(e.getMessage());
        }

        return null;
    }

    private void removeHandler(ChannelPipeline channelPipeline, String handlerName) {
        try {
            channelPipeline.remove(handlerName);
        } catch (NoSuchElementException e) {
            log.error(e.getMessage());
        }
    }

    private ChannelFuture connect(String host, int port, ChannelHandler channelHandler) {
        return bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 15_000)
                .option(ChannelOption.SO_KEEPALIVE, false)
                .group(clientContext.channel().eventLoop()).channel(NioSocketChannel.class)
                .handler(channelHandler).connect(host, port);
    }
}
