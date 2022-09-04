package me.bristermitten.mittenlib.commands;

import co.aikar.commands.PaperCommandManager;
import me.bristermitten.mittenlib.commands.handlers.*;
import org.bukkit.plugin.Plugin;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Set;

/**
 * A provider for {@link PaperCommandManager}.
 * This handles registration of {@link Command}, {@link TabCompleter}, {@link NamedCondition}, {@link ArgumentContext} and {@link ArgumentCondition} instances.
 */
public class CommandManagerProvider implements Provider<PaperCommandManager> {
    private final Plugin plugin;
    private final Set<Command> commands;
    private final Set<TabCompleter> tabCompleters;
    private final Set<NamedCondition> namedConditions;
    private final Set<ArgumentCondition<?>> argumentConditions;
    private final Set<ArgumentContext<?>> argumentContexts;


    @Inject
    CommandManagerProvider(Plugin plugin, Set<Command> commands, Set<TabCompleter> tabCompleters, Set<ArgumentCondition<?>> argumentConditions, Set<NamedCondition> namedConditions, Set<ArgumentContext<?>> argumentContexts) {
        this.plugin = plugin;
        this.commands = commands;
        this.tabCompleters = tabCompleters;
        this.argumentConditions = argumentConditions;
        this.namedConditions = namedConditions;
        this.argumentContexts = argumentContexts;
    }

    private void registerTabCompleter(PaperCommandManager manager, TabCompleter completer) {
        manager.getCommandCompletions().registerAsyncCompletion(completer.id(), completer);
    }

    private <T> void registerContext(PaperCommandManager manager, ArgumentContext<T> context) {
        if (context instanceof TabCompleter) {
            TabCompleter completer = (TabCompleter) context;
            registerTabCompleter(manager, completer);
            manager.getCommandCompletions().setDefaultCompletion(completer.id(), context.type());
        }
        if (context instanceof IssuerAwareArgumentContext) {
            manager.getCommandContexts().registerIssuerAwareContext(context.type(), (IssuerAwareArgumentContext<T>) context);
        } else if (context instanceof IssuerOnlyArgumentContext) {
            manager.getCommandContexts().registerIssuerOnlyContext(context.type(), (IssuerOnlyArgumentContext<T>) context);
        } else {
            manager.getCommandContexts().registerContext(context.type(), context);
        }
    }

    private <T> void registerCondition(PaperCommandManager manager, ArgumentCondition<T> condition) {
        manager.getCommandConditions().addCondition(condition.type(), condition.id(), condition);
    }

    private void registerNamedCondition(PaperCommandManager manager, NamedCondition namedCondition) {
        manager.getCommandConditions().addCondition(namedCondition.id(), namedCondition);
    }

    @Override
    public PaperCommandManager get() {
        PaperCommandManager paperCommandManager = new PaperCommandManager(plugin);
        paperCommandManager.enableUnstableAPI("help");

        for (TabCompleter tabCompleter : tabCompleters) {
            registerTabCompleter(paperCommandManager, tabCompleter);
        }
        for (ArgumentContext<?> argumentContext : argumentContexts) {
            registerContext(paperCommandManager, argumentContext);
        }
        for (ArgumentCondition<?> argumentCondition : argumentConditions) {
            registerCondition(paperCommandManager, argumentCondition);
        }
        for (NamedCondition namedCondition : namedConditions) {
            registerNamedCondition(paperCommandManager, namedCondition);
        }

        for (Command command : commands) {
            paperCommandManager.registerCommand(command);
        }

        return paperCommandManager;
    }
}
