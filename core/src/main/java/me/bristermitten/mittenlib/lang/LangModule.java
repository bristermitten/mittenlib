package me.bristermitten.mittenlib.lang;

import com.google.gson.TypeAdapterFactory;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;
import me.bristermitten.mittenlib.lang.format.MessageFormatter;
import me.bristermitten.mittenlib.lang.format.SimpleMessageFormatter;
import me.bristermitten.mittenlib.lang.format.hook.HookModule;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;

public class LangModule extends AbstractModule {
    @Override
    protected void configure() {
        Multibinder.newSetBinder(binder(), TypeAdapterFactory.class)
                .addBinding().to(LangMessageTypeAdapterFactory.class);

        bind(BukkitAudiences.class).toProvider(AdventureAudienceProvider.class).in(Scopes.SINGLETON);
        bind(MessageFormatter.class).to(SimpleMessageFormatter.class);

        install(new HookModule());
    }
}
