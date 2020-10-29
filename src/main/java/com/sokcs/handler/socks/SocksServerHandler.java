package com.sokcs.handler.socks;

import com.sokcs.utils.SocksServerUtils;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.SocksMessage;
import io.netty.handler.codec.socksx.v4.Socks4CommandRequest;
import io.netty.handler.codec.socksx.v4.Socks4CommandType;
import io.netty.handler.codec.socksx.v5.*;

@ChannelHandler.Sharable
public final class SocksServerHandler extends SimpleChannelInboundHandler<SocksMessage> {

    public static final SocksServerHandler INSTANCE = new SocksServerHandler();

    private static final boolean flag = true;

    private SocksServerHandler() {
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, SocksMessage socksRequest) throws Exception {
        switch (socksRequest.version()) {
            case SOCKS4a:
                Socks4CommandRequest socksV4CmdRequest = (Socks4CommandRequest) socksRequest;
                if (socksV4CmdRequest.type() == Socks4CommandType.CONNECT) {
                    ctx.pipeline().addLast(new SocksServerConnectHandler());
                    ctx.pipeline().remove(this);
                    ctx.fireChannelRead(socksRequest);
                } else {
                    ctx.close();
                }
                break;
            case SOCKS5:
                if (socksRequest instanceof Socks5InitialRequest) {
                    if (flag) {
                        ctx.pipeline().addFirst(new Socks5PasswordAuthRequestDecoder());
                        ctx.writeAndFlush(new DefaultSocks5InitialResponse(Socks5AuthMethod.PASSWORD));
                    } else {
                        ctx.pipeline().addFirst(new Socks5CommandRequestDecoder());
                        ctx.writeAndFlush(new DefaultSocks5InitialResponse(Socks5AuthMethod.NO_AUTH));
                    }

                } else if (socksRequest instanceof Socks5PasswordAuthRequest) {
                    if (flag) {
                        Socks5PasswordAuthRequest passwordAuthRequest = (Socks5PasswordAuthRequest) socksRequest;
                        if (passwordAuthRequest.username().equals("aa") &&
                                passwordAuthRequest.password().equals("aa")) {
                            ctx.pipeline().addFirst( new Socks5CommandRequestDecoder());

                            ctx.write(new DefaultSocks5PasswordAuthResponse(Socks5PasswordAuthStatus.SUCCESS));
                        } else {
                            ctx.write(new DefaultSocks5PasswordAuthResponse(Socks5PasswordAuthStatus.FAILURE));
                            SocksServerUtils.closeOnFlush(ctx.channel());
                        }
                    } else {
                        ctx.pipeline().addFirst(new Socks5CommandRequestDecoder());
                        ctx.write(new DefaultSocks5PasswordAuthResponse(Socks5PasswordAuthStatus.SUCCESS));
                    }
                } else if (socksRequest instanceof Socks5CommandRequest) {
                    Socks5CommandRequest socks5CmdRequest = (Socks5CommandRequest) socksRequest;
                    if (socks5CmdRequest.type() == Socks5CommandType.CONNECT) {
                        ctx.pipeline().addLast(new SocksServerConnectHandler());
                        ctx.pipeline().remove(this);
                        ctx.fireChannelRead(socksRequest);
                    } else {
                        ctx.close();
                    }
                } else {
                    ctx.close();
                }
                break;
            default:
                ctx.close();
                break;
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable throwable) {
        throwable.printStackTrace();
        SocksServerUtils.closeOnFlush(ctx.channel());
    }
}