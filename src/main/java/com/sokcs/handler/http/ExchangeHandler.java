package com.sokcs.handler.http;

import com.sokcs.utils.ChannelUtils;
import com.sokcs.utils.ThrowableUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import lombok.extern.slf4j.Slf4j;

/**
 * 负责交换通道数据，不捕获内容
 */
@Slf4j
public class ExchangeHandler extends ChannelInboundHandlerAdapter {
    private final Channel outputChannel;

    public ExchangeHandler(Channel targetChannel) {
        this.outputChannel = targetChannel;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ThrowableUtils.message(this.getClass(), cause);
        ctx.close();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ChannelUtils.writeAndFlush(outputChannel, msg);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }
}
