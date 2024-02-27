package io.xarate.protocols.stomp;

import java.nio.charset.Charset;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.ReferenceCountUtil;

public class BytesToWebSocketFrameEncoder extends ChannelOutboundHandlerAdapter {
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof ByteBuf) {
            String frameContent = ((ByteBuf) msg).toString(Charset.forName("UTF-8"));
            ReferenceCountUtil.retain(msg);
            ctx.write(new TextWebSocketFrame(frameContent), promise);
        } else {
            ctx.write(ReferenceCountUtil.retain(msg), promise);
        }
    }
}