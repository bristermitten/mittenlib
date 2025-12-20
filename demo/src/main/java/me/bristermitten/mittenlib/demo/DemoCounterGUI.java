package me.bristermitten.mittenlib.demo;

import com.google.inject.Inject;
import me.bristermitten.mittenlib.gui.UpdateResult;
import me.bristermitten.mittenlib.gui.factory.MinecraftGUIFactory;
import me.bristermitten.mittenlib.gui.spigot.SpigotGUI;
import me.bristermitten.mittenlib.gui.spigot.SpigotGUIView;
import me.bristermitten.mittenlib.gui.spigot.command.SpigotCommand;
import me.bristermitten.mittenlib.gui.spigot.command.SpigotCommandContext;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Improved counter GUI that uses dependency injection instead of static methods.
 * Demonstrates the new Elm-like architecture with proper Guice integration.
 */
public class DemoCounterGUI extends SpigotGUI<Counter, CounterMessage> {

    private final MinecraftGUIFactory minecraftGuiFactory;

    @Inject
    public DemoCounterGUI(MinecraftGUIFactory minecraftGuiFactory) {
        this.minecraftGuiFactory = minecraftGuiFactory;
    }

    @Override
    public @NotNull Counter init() {
        return Counter.create(0);
    }

    @Override
    public @NotNull UpdateResult<Counter, CounterMessage, SpigotCommandContext<CounterMessage>, SpigotCommand<CounterMessage>> update(Counter counter, CounterMessage message) {
        return message.matchTo(
                increment -> UpdateResult.pure(Counter.create(counter.value() + 1)),
                decrement -> UpdateResult.pure(Counter.create(counter.value() - 1)),
                setValue -> UpdateResult.pure(Counter.create(setValue.value())),
                displayCount -> UpdateResult.pure(counter)
        );
    }

    @Override
    public @NotNull SpigotGUIView<CounterMessage> render(Counter counter) {
        return minecraftGuiFactory.<CounterMessage>createSpigotView(27, "Count: " + counter.value())
                .onClose(CounterMessage.DisplayCount())
                .withButton(10, minecraftGuiFactory.createButton(
                        new ItemStack(Material.EMERALD),
                        CounterMessage.Increment(),
                        "§a+1 (Click to increment)"
                ))
                .withButton(13, minecraftGuiFactory.createButton(
                        new ItemStack(Material.GOLD_INGOT),
                        CounterMessage.Set(0),
                        "§eReset (Click to reset to 0)"
                ))
                .withButton(16, minecraftGuiFactory.createButton(
                        new ItemStack(Material.REDSTONE),
                        CounterMessage.Decrement(),
                        "§c-1 (Click to decrement)"
                ))
                .withButton(22, minecraftGuiFactory.createButton(
                        new ItemStack(Material.DIAMOND),
                        CounterMessage.Set(100),
                        "§b+100 (Click to set to 100)"
                ));
    }
}