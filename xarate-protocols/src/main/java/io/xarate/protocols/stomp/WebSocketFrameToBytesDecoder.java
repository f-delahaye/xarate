package io.xarate.protocols.stomp;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.ReferenceCountUtil;

public class WebSocketFrameToBytesDecoder extends SimpleChannelInboundHandler<WebSocketFrame> {

    @Override
    public void channelRead0(ChannelHandlerContext ctx, WebSocketFrame msg) throws Exception {
        // extract the ByteBuf and send it down to the Stomp Decoder
        ctx.fireChannelRead(ReferenceCountUtil.retain(msg).content());
    }
}