package me.bristermitten.mittenlib.gui.event;

/**
 * Represents a subscription to events that can be cancelled.
 */
public interface Subscription {

    /**
     * Cancels this subscription, preventing further events from being delivered.
     */
    void cancel();

    /**
     * Returns whether this subscription is still active.
     *
     * @return true if the subscription is active, false if cancelled
     */
    boolean isActive();
}