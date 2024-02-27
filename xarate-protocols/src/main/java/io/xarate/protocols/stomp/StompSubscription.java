package io.xarate.protocols.stomp;

import java.util.function.Predicate;

public interface StompSubscription {
    Object listen(Predicate<Object> filter, String delayExpression);

    Object listen(String delayExpression);
}
