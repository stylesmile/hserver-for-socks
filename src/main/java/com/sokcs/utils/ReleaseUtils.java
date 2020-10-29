package com.sokcs.utils;

import io.netty.buffer.ByteBuf;

public final class ReleaseUtils {
    public static void release(Object msg) {
        if (!(msg instanceof ByteBuf)) {
            return;
        }

        ByteBuf byteBuf = (ByteBuf) msg;
        if (byteBuf.refCnt() > 0) {
            byteBuf.release();
        }
    }
}
