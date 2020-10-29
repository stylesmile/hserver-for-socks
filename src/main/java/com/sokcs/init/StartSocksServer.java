package com.sokcs.init;

import com.sokcs.certificate.CertificateImpl;
import com.sokcs.certificate.CertificatePool;
import com.sokcs.handler.http.HandlerName;
import com.sokcs.handler.http.HttpHandler;
import com.sokcs.handler.http.HttpProxyAuthorizationHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;
import top.hserver.core.interfaces.InitRunner;
import top.hserver.core.ioc.annotation.Bean;

/**
 * @author hxm
 */
@Bean
@Slf4j
public class StartSocksServer implements InitRunner {

    private static final Integer SOCKS_PORT = 19160;
    private static final Integer HTTP_PORT = 19161;

    @Override
    public void init(String[] strings) {
        startSocksProxy();
        startHttpSocks();
    }


    public void startSocksProxy() {
        new Thread() {
            @Override
            public void run() {
                EventLoopGroup bossGroup = new NioEventLoopGroup(1);
                EventLoopGroup workerGroup = new NioEventLoopGroup();
                try {
                    ServerBootstrap b = new ServerBootstrap();
                    b.group(bossGroup, workerGroup)
                            .channel(NioServerSocketChannel.class)
                            .handler(new LoggingHandler(LogLevel.INFO))
                            .childHandler(new SocksServerInitializer());
                    try {
                        log.info("Socks代理服务启动成功，运行于 {} 端口", SOCKS_PORT);
                        b.bind(SOCKS_PORT).sync().channel().closeFuture().sync();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } finally {
                    bossGroup.shutdownGracefully();
                    workerGroup.shutdownGracefully();
                }
            }
        }.start();
    }

    public void startHttpSocks() {
        new Thread() {
            @Override
            public void run() {
                EventLoopGroup bossGroup = new NioEventLoopGroup();
                EventLoopGroup workerGroup = new NioEventLoopGroup();
                try {
                    ServerBootstrap serverBootstrap = new ServerBootstrap();
                    serverBootstrap.group(bossGroup, workerGroup)
                            .channel(NioServerSocketChannel.class)
                            .childHandler(new ChannelInitializer<SocketChannel>() {
                                @Override
                                protected void initChannel(SocketChannel socketChannel) throws Exception {
                                    ChannelPipeline channelPipeline = socketChannel.pipeline();

                                    channelPipeline.addLast(HandlerName.HTTP_SERVER_CODEC, new HttpServerCodec());
                                    channelPipeline.addLast(HandlerName.HTTP_OBJECT_AGGREGATOR, new HttpObjectAggregator(1024 * 1024 * 512));
                                    channelPipeline.addLast(HandlerName.HTTP_AUTH, new HttpProxyAuthorizationHandler());
                                    channelPipeline.addLast(HandlerName.HTTP_HANDLER, new HttpHandler(null,null,null));
                                }
                            });
                    ChannelFuture channelFuture = serverBootstrap.bind(HTTP_PORT).sync();

                    log.info("Http代理服务启动成功，运行于 {} 端口", HTTP_PORT);

                    channelFuture.channel().closeFuture().sync();
                } catch (InterruptedException e) {
                    log.error(e.getMessage());
                } finally {
                    workerGroup.shutdownGracefully();
                    bossGroup.shutdownGracefully();
                }

            }
        }.start();


    }


}
