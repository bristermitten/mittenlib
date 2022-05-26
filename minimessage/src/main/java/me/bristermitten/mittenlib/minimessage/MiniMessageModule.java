package me.bristermitten.mittenlib.minimessage;

import com.google.inject.multibindings.Multibinder;
import me.bristermitten.mittenlib.lang.LangModule;
import me.bristermitten.mittenlib.lang.format.AbstractMiniMessageFactory;
import me.bristermitten.mittenlib.lang.format.DefaultMiniMessageFactory;
import me.bristermitten.mittenlib.lang.format.MessageFormatter;
import me.bristermitten.mittenlib.lang.format.MiniMessageFormatter;
import me.bristermitten.mittenlib.lang.format.hook.FormattingHook;
import me.bristermitten.mittenlib.lang.format.hook.LegacyCodeUpdaterHook;

public class MiniMessageModule extends LangModule {
    @Override
    protected void configure() {
        bind(MessageFormatter.class).to(MiniMessageFormatter.class);
        bind(AbstractMiniMessageFactory.class).to(DefaultMiniMessageFactory.class);

        Multibinder.newSetBinder(binder(), FormattingHook.class)
                .addBinding().to(LegacyCodeUpdaterHook.class);
    }

}
