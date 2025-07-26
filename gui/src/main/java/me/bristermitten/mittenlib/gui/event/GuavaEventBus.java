package me.bristermitten.mittenlib.gui.event;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Singleton;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * EventBus implementation using Google's Guava EventBus.
 * Provides reactive event handling with type-safe subscriptions.
 */
@Singleton
public class GuavaEventBus implements EventBus {

    private final com.google.common.eventbus.EventBus eventBus;
    private final Map<Long, SubscriptionImpl<?>> subscriptions;
    private final AtomicLong subscriptionIdGenerator;

    public GuavaEventBus() {
        this.eventBus = new com.google.common.eventbus.EventBus("GUI-EventBus");
        this.subscriptions = new ConcurrentHashMap<>();
        this.subscriptionIdGenerator = new AtomicLong(0);
    }

    @Override
    public void publish(Object event) {
        eventBus.post(event);
    }

    @Override
    public <T> Subscription subscribe(Class<T> eventType, Consumer<T> handler) {
        long id = subscriptionIdGenerator.incrementAndGet();
        SubscriptionImpl<T> subscription = new SubscriptionImpl<>(id, eventType, handler, this);
        subscriptions.put(id, subscription);
        eventBus.register(subscription);
        return subscription;
    }

    @Override
    public void register(Object subscriber) {
        eventBus.register(subscriber);
    }

    @Override
    public void unregister(Object subscriber) {
        eventBus.unregister(subscriber);
    }

    private void removeSubscription(long id) {
        SubscriptionImpl<?> subscription = subscriptions.remove(id);
        if (subscription != null) {
            eventBus.unregister(subscription);
        }
    }

    private static class SubscriptionImpl<T> implements Subscription {
        private final long id;
        private final Class<T> eventType;
        private final Consumer<T> handler;
        private final GuavaEventBus eventBus;
        private volatile boolean active = true;

        public SubscriptionImpl(long id, Class<T> eventType, Consumer<T> handler, GuavaEventBus eventBus) {
            this.id = id;
            this.eventType = eventType;
            this.handler = handler;
            this.eventBus = eventBus;
        }

        @Subscribe
        public void handleEvent(Object event) {
            if (active && eventType.isInstance(event)) {
                handler.accept(eventType.cast(event));
            }
        }

        @Override
        public void cancel() {
            if (active) {
                active = false;
                eventBus.removeSubscription(id);
            }
        }

        @Override
        public boolean isActive() {
            return active;
        }
    }
}