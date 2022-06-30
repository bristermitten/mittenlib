package me.bristermitten.mittenlib.papi;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import me.bristermitten.mittenlib.lang.format.hook.FormattingHook;
import me.bristermitten.mittenlib.lang.hook.PAPIFormattingHook;


/**
 * Binds PAPI related classes
 */
public class PAPIModule extends AbstractModule {
    @Override
    protected void configure() {
        final Multibinder<FormattingHook> chatHookMultibinder = Multibinder.newSetBinder(binder(), FormattingHook.class);
        chatHookMultibinder.addBinding().to(PAPIFormattingHook.class);
    }
}
