package io.xarate.protocols.stomp;

import com.intuit.karate.http.WebSocketOptions;

public interface StompClient extends AutoCloseable {

    public static StompClient overWebSocket(WebSocketOptions options) {
        return new StompNettyClient(options, null);
    }

    public StompSubscription subscribe(String topic);

    @Override
    public void close();
}
