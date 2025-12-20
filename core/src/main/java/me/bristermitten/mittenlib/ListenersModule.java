package me.bristermitten.mittenlib;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import org.bukkit.event.Listener;

/**
 * Creates a new empty {@link Multibinder} for Bukkit {@link Listener}s and binds {@link ListenerRegistration} as an eager singleton.
 * This causes all registered listeners to be registered with the Bukkit plugin manager upon Injector creation automatically.
 */
public class ListenersModule extends AbstractModule {

    @Override
    protected void configure() {
        Multibinder.newSetBinder(binder(), Listener.class);
        bind(ListenerRegistration.class).asEagerSingleton();
    }
}
