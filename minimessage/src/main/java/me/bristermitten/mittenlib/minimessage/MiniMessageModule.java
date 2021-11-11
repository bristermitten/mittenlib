package me.bristermitten.mittenlib.minimessage;

import me.bristermitten.mittenlib.lang.LangModule;
import me.bristermitten.mittenlib.lang.format.AbstractMiniMessageFactory;
import me.bristermitten.mittenlib.lang.format.DefaultMiniMessageFactory;
import me.bristermitten.mittenlib.lang.format.MessageFormatter;
import me.bristermitten.mittenlib.lang.format.MiniMessageFormatter;

public class MiniMessageModule extends LangModule {
    @Override
    protected void configure() {
        bind(MessageFormatter.class).to(MiniMessageFormatter.class);
        bind(AbstractMiniMessageFactory.class).to(DefaultMiniMessageFactory.class);
    }

}
