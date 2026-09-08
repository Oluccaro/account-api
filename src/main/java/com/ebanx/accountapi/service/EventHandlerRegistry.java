package com.ebanx.accountapi.service;

import com.ebanx.accountapi.domain.EventType;
import com.ebanx.accountapi.service.handlers.EventHandler;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Fails at startup rather than at request time when an event type has no handler, or more than
 * one. The set of supported operations is therefore a boot-time invariant.
 */
@Component
public class EventHandlerRegistry {

    private final Map<EventType, EventHandler> handlers = new EnumMap<>(EventType.class);

    public EventHandlerRegistry(List<EventHandler> availableHandlers) {
        for (EventHandler handler : availableHandlers) {
            EventHandler previous = handlers.put(handler.type(), handler);
            if (previous != null) {
                throw new IllegalStateException("More than one handler for " + handler.type());
            }
        }

        List<EventType> unhandled = Arrays.stream(EventType.values())
                .filter(type -> !handlers.containsKey(type))
                .toList();

        if (!unhandled.isEmpty()) {
            throw new IllegalStateException("No handler for " + unhandled);
        }
    }

    public EventHandler resolve(EventType type) {
        return handlers.get(type);
    }
}
