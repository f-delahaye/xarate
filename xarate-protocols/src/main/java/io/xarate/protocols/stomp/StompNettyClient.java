package io.xarate.protocols.stomp;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.net.ssl.SSLException;

import com.intuit.karate.Logger;
import com.intuit.karate.http.WebSocketOptions;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketHandshakeException;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler;
import io.netty.handler.codec.stomp.DefaultStompFrame;
import io.netty.handler.codec.stomp.StompCommand;
import io.netty.handler.codec.stomp.StompFrame;
import io.netty.handler.codec.stomp.StompHeaders;
import io.netty.handler.codec.stomp.StompSubframeAggregator;
import io.netty.handler.codec.stomp.StompSubframeDecoder;
import io.netty.handler.codec.stomp.StompSubframeEncoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.util.ReferenceCountUtil;

public class StompNettyClient implements StompClient {

    private static final AtomicLong COUNTER = new AtomicLong(0);

    private NioEventLoopGroup group;
    private Logger logger;
    private Channel channel;
    private WebSocketClientHandshaker handshaker;
    private ChannelPromise handshakeFuture;
    private ChannelPromise connectionFuture;

    private final Map<String, StompNettySubscription> subscriptions = new LinkedHashMap<>();

    public StompNettyClient(String wsUrl) {
        this(new WebSocketOptions(wsUrl), new Logger(StompNettyClient.class));
    }

    public StompNettyClient(WebSocketOptions options, Logger logger) {
        this.logger = logger;
        URI uri = options.getUri();
        int port = options.getPort();
        group = new NioEventLoopGroup();
        SslContext sslContext;
        if (options.isSsl()) {
            try {
                sslContext = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
            } catch (SSLException e) {
                throw new RuntimeException(e);
            }
        } else {
            sslContext = null;
        }
        HttpHeaders nettyHeaders = new DefaultHttpHeaders();
        Map<String, Object> headers = options.getHeaders();
        if (headers != null) {
            headers.forEach((k, v) -> nettyHeaders.add(k, v));
        }

        handshaker = WebSocketClientHandshakerFactory.newHandshaker(
                uri, WebSocketVersion.V13, options.getSubProtocol(), true, nettyHeaders, options.getMaxPayloadSize());
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<>() {
                        @Override
                        protected void initChannel(Channel c) {
                            ChannelPipeline pipeline = c.pipeline();
                            if (sslContext != null) {
                                pipeline.addLast(sslContext.newHandler(c.alloc(), uri.getHost(), port));
                            }
                            pipeline
                                .addLast(new HttpClientCodec())
                                .addLast(new HttpObjectAggregator(8192))
                                .addLast(WebSocketClientCompressionHandler.INSTANCE)
                                .addLast(new WebSocketHandshakeClientHandler())
                                .addLast(new WebSocketFrameToBytesDecoder())
                                .addLast(new BytesToWebSocketFrameEncoder())
                                .addLast(new StompSubframeDecoder())
                                .addLast(new StompSubframeEncoder())
                                .addLast(new StompSubframeAggregator(1048576))
                                .addLast(new StompClientHandler());        
                        }                
                    });
            channel = b.connect(options.getUri().getHost(), options.getPort()).sync().channel();
            handshakeFuture.sync();
            connect();
            connectionFuture.sync();
        } catch (Exception e) {
            logger.error("websocket client init failed: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    // Private method. Will be called automatically (and asynchronously) upon
    // handshake completion
    private void connect() {
        logger.info("Client connecting...");
        StompFrame connectFrame = new DefaultStompFrame(StompCommand.CONNECT);
        connectFrame.headers().set(StompHeaders.ACCEPT_VERSION, "1.2");
        connectionFuture = channel.newPromise();
        channel.writeAndFlush(connectFrame);
    }

    /*
    * Creates a subscription. But messages will not be sent to the subscription until {@link io.xarate.protocols.stomp.StompSubscription#listen} is called.
    * */
    public StompNettySubscription subscribe(String topic) {
        String subscriptionId = String.valueOf(COUNTER.incrementAndGet());
        return new StompNettySubscription(this, topic, subscriptionId);
    }

    protected void startListening(StompNettySubscription subscription) {

        String topic = subscription.getTopic();
        String subscriptionId = subscription.getId();

        logger.info("Client listening...");
        subscriptions.put(subscriptionId, subscription);

        StompFrame subscribeFrame = new DefaultStompFrame(StompCommand.SUBSCRIBE);
        subscribeFrame.headers().set(StompHeaders.DESTINATION, topic);
        subscribeFrame.headers().set(StompHeaders.ID, subscriptionId);
        channel.writeAndFlush(subscribeFrame);
    }

    protected void stopListening(StompNettySubscription subscription) {
        subscriptions.remove(subscription.getId());
    }

    @Override
    public void close() {
        if (channel.isOpen()) {
            logger.debug("Close request");
            try {
                channel.writeAndFlush(new CloseWebSocketFrame());
                logger.debug("Waiting close future");
                channel.closeFuture().sync();
                logger.debug("Now closing");
                group.shutdownGracefully();
            } catch (InterruptedException e) {
                // NOOP
            }    
        }
    }

    private class WebSocketHandshakeClientHandler extends SimpleChannelInboundHandler<Object> {
        @Override
        public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
            Channel ch = ctx.channel();
            if (!handshaker.isHandshakeComplete()) {
                try {
                    handshaker.finishHandshake(ch, (FullHttpResponse) msg);
                    logger.debug("Websocket client connected");
                    handshakeFuture.setSuccess();
                } catch (WebSocketHandshakeException e) {
                    logger.debug("Websocket client connect failed: {}", e.getMessage());
                    handshakeFuture.setFailure(e);
                }
            } else {
                ReferenceCountUtil.retain(msg);                    
                ctx.fireChannelRead(msg);
            }
        }


        @Override
        public void handlerAdded(ChannelHandlerContext ctx) {
            handshakeFuture = ctx.newPromise();
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            handshaker.handshake(ctx.channel());
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            logger.debug("Websocket client disconnected");
        }

    }

    private class StompClientHandler extends SimpleChannelInboundHandler<StompFrame> {
        @Override
        public void channelRead0(ChannelHandlerContext ctx, StompFrame frame) throws Exception {
            String subscriptionId = null;
            if (frame.headers().get(StompHeaders.SUBSCRIPTION) != null) {
                subscriptionId = frame.headers().get(StompHeaders.SUBSCRIPTION).toString();
            }
            switch (frame.command()) {
                case CONNECTED:
                    logger.info("Client connected");
                    connectionFuture.setSuccess();
                    break;
                default:
                    logger.debug("Received frame {} for {}", frame, subscriptionId);
                    subscriptions.get(subscriptionId).onStompFrame(frame);
                    break;
            }
        }

    }
}
