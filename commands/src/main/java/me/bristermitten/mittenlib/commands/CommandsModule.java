package me.bristermitten.mittenlib.commands;

import co.aikar.commands.BukkitCommandManager;
import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import me.bristermitten.mittenlib.commands.handlers.ArgumentCondition;
import me.bristermitten.mittenlib.commands.handlers.ArgumentContext;
import me.bristermitten.mittenlib.commands.handlers.NamedCondition;

public class CommandsModule extends AbstractModule {
    @Override
    protected void configure() {
        // Create empty multibinders so that an error isn't thrown if none are registered
        Multibinder.newSetBinder(binder(), Command.class);
        Multibinder.newSetBinder(binder(), NamedCondition.class);
        Multibinder.newSetBinder(binder(), new TypeLiteral<ArgumentContext<?>>() {
        });
        Multibinder.newSetBinder(binder(), new TypeLiteral<ArgumentCondition<?>>() {
        });


        bind(BukkitCommandManager.class).toProvider(CommandManagerProvider.class).asEagerSingleton();
    }
}
