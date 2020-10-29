package com.sokcs.handler.http;

import com.sokcs.utils.ChannelUtils;
import com.sokcs.utils.ReleaseUtils;
import com.sokcs.utils.ThrowableUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

@Slf4j
public class CaptureExchangeHandler extends ChannelInboundHandlerAdapter {
    private Consumer<FullHttpRequest> consumer;
    private final Channel outputChannel;

    private final String desc;

    public CaptureExchangeHandler(Consumer<FullHttpRequest> consumer, Channel outputChannel, String desc) {
        this.consumer = consumer;
        this.outputChannel = outputChannel;
        this.desc = desc;
    }

    public CaptureExchangeHandler(Channel outputChannel, String desc) {
        this.outputChannel = outputChannel;
        this.desc = desc;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            FullHttpRequest fullHttpRequest = (FullHttpRequest) msg;

            consumer.accept(fullHttpRequest);

            ChannelUtils.writeAndFlush(outputChannel, fullHttpRequest);
            ReleaseUtils.release(fullHttpRequest);
        } else if (msg instanceof FullHttpResponse) {
            ChannelUtils.writeAndFlush(outputChannel, msg);
        }
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ThrowableUtils.message(desc, this.getClass(), cause);
        ctx.close();
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }
}
