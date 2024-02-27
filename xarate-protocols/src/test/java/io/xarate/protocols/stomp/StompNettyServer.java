package io.xarate.protocols.stomp;

import java.net.InetSocketAddress;

import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;
import io.netty.handler.codec.stomp.DefaultStompFrame;
import io.netty.handler.codec.stomp.StompCommand;
import io.netty.handler.codec.stomp.StompFrame;
import io.netty.handler.codec.stomp.StompHeaders;
import io.netty.handler.codec.stomp.StompSubframeAggregator;
import io.netty.handler.codec.stomp.StompSubframeDecoder;
import io.netty.handler.codec.stomp.StompSubframeEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class StompNettyServer {

    private static final Logger LOG = LoggerFactory.getLogger(StompNettyServer.class);
    
    private final String path;
    private final int requestedPort;

    private Channel ch;
    private NioEventLoopGroup bossGroup;
    private NioEventLoopGroup workerGroup;

    public StompNettyServer(int requestedPort, String path) {
        this.requestedPort = requestedPort;
        this.path = path;
    }

    public int getPort() {
        return ((InetSocketAddress)ch.localAddress()).getPort();
    }

    public void run() {
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            .handler(new LoggingHandler(LogLevel.INFO))
            .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline()                                            
                    .addLast(new HttpServerCodec())
                    .addLast(new HttpObjectAggregator(65536))
                    .addLast(new WebSocketServerCompressionHandler())                  
                    // WebSocketServerProtocolHandler auto registers a WebSocketServerProtocolHandshakeHandler                                                                   
                    .addLast(new WebSocketServerProtocolHandler(path, null, true))
                     .addLast(new WebSocketFrameToBytesDecoder())
                     .addLast(new BytesToWebSocketFrameEncoder())
                    .addLast(new StompSubframeDecoder())
                    .addLast(new StompSubframeEncoder())
                    .addLast(new StompSubframeAggregator(1048576))
                    .addLast(new StompServerHandler())
;        

                }
            }).option(ChannelOption.SO_BACKLOG, 128).childOption(ChannelOption.SO_KEEPALIVE, true);

            ch = b.bind(requestedPort).sync().channel();

            LOG.info("Server started");
        } catch (Exception exc) {
            throw new RuntimeException(exc); 
        }
    }

    public void close() {
        try {
            ch.writeAndFlush(new CloseWebSocketFrame());
            ch.close().sync();
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private static class StompServerHandler extends SimpleChannelInboundHandler<StompFrame> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, StompFrame frame) throws Exception {
            LOG.info("received "+frame);
            switch (frame.command()) {
                case CONNECT: 
                    ctx.writeAndFlush(new DefaultStompFrame(StompCommand.CONNECTED)); 
                    break;
                case SUBSCRIBE:                
                    StompHeaders headers = frame.headers();
                    ByteBuf content = ctx.alloc().buffer().writeBytes(("Subscribed to "+headers.get(StompHeaders.DESTINATION)).getBytes());                    
                    DefaultStompFrame testMessageFrame = new DefaultStompFrame(StompCommand.MESSAGE, content);
                    testMessageFrame.headers().set(StompHeaders.SUBSCRIPTION, headers.get(StompHeaders.ID));
                    ctx.writeAndFlush(testMessageFrame);
            }
        }
        
    }
}
