package me.bristermitten.mittenlib.gui.event;

import java.util.function.Consumer;

/**
 * Event bus interface for reactive event handling in the GUI framework.
 * Provides a decoupled way to publish and subscribe to events.
 */
public interface EventBus {

    /**
     * Publishes an event to all registered subscribers.
     *
     * @param event the event to publish
     */
    void publish(Object event);

    /**
     * Subscribes to events of a specific type.
     *
     * @param eventType the type of events to subscribe to
     * @param handler   the handler to call when events of this type are published
     * @param <T>       the event type
     * @return a subscription that can be used to unsubscribe
     */
    <T> Subscription subscribe(Class<T> eventType, Consumer<T> handler);

    /**
     * Registers an object with @Subscribe annotated methods.
     *
     * @param subscriber the subscriber object
     */
    void register(Object subscriber);

    /**
     * Unregisters a previously registered subscriber.
     *
     * @param subscriber the subscriber to unregister
     */
    void unregister(Object subscriber);
}