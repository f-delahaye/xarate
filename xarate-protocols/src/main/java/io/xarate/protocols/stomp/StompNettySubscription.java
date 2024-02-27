package io.xarate.protocols.stomp;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.handler.codec.stomp.StompFrame;

public class StompNettySubscription implements StompSubscription {

    private static final Logger LOG = LoggerFactory.getLogger(StompNettySubscription.class);
    
    private static final Predicate<Object> CATCH_ALL = obj -> true;
    private final StompNettyClient client;
    private final String topic;
    private final String id;

    private CompletableFuture<String> messageFuture;

    private Predicate<Object> filter;

    StompNettySubscription(StompNettyClient client, String topic, String id) {
        this.client = client;
        this.topic = topic;
        this.id = id;
    }

    protected String getTopic() {
        return topic;
    }

    protected String getId() {
        return id;
    }

    @Override
    public Object listen(String delayExpression) {
        return listen(CATCH_ALL, delayExpression);
    }
    
    @Override
    public Object listen(Predicate<Object> filter, String delayExpression) {
        long delay = Integer.parseInt(delayExpression);
        return listen(filter, delay);
    }

    private Object listen(Predicate<Object> filter, long delay) {
        this.filter = filter == null ? CATCH_ALL : filter;
        client.startListening(this);

        this.messageFuture = new CompletableFuture<>();

        try {
            return messageFuture.get(delay, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } catch (ExecutionException | TimeoutException e) {
            throw new IllegalStateException(e);
        }
    }
    
    public void onStompFrame(StompFrame frame) {
            switch (frame.command()) {
                case MESSAGE:
                    String message = frame.content().toString(StandardCharsets.UTF_8);
                    LOG.debug("Received {}", message);
                    if (filter.test(message)) {
                        messageFuture.complete(message);
                        client.stopListening(this);
                    }
                    break;
                case ERROR:
                    messageFuture.completeExceptionally(new RuntimeException());
                    client.stopListening(this);
            }
        }

    
}
