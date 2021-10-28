package me.bristermitten.mittenlib.lang.format.hook;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

public class HookModule extends AbstractModule {
    @Override
    protected void configure() {
        final Multibinder<FormattingHook> chatHookMultibinder = Multibinder.newSetBinder(binder(), FormattingHook.class);
        chatHookMultibinder.addBinding().to(HexColorFixerHook.class);
        chatHookMultibinder.addBinding().to(SimpleColorCodeHook.class);
    }
}
