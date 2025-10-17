package me.bristermitten.mittenlib.demo;

import com.google.inject.Inject;
import me.bristermitten.mittenlib.gui.UpdateResult;
import me.bristermitten.mittenlib.gui.factory.MinecraftGUIFactory;
import me.bristermitten.mittenlib.gui.spigot.SpigotGUI;
import me.bristermitten.mittenlib.gui.spigot.SpigotGUIView;
import me.bristermitten.mittenlib.gui.spigot.command.SpigotCommand;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Improved counter GUI that uses dependency injection instead of static methods.
 * Demonstrates the new Elm-like architecture with proper Guice integration.
 */
public class DemoCounterGUI extends SpigotGUI<Counter, CounterMessage, Object> {

    private final MinecraftGUIFactory minecraftGuiFactory;

    @Inject
    public DemoCounterGUI(MinecraftGUIFactory minecraftGuiFactory) {
        this.minecraftGuiFactory = minecraftGuiFactory;
    }

    @Override
    public Counter init() {
        return Counter.create(0);
    }

    @Override
    public UpdateResult<Counter, CounterMessage, SpigotCommand<CounterMessage>> update(Counter counter, CounterMessage message) {
//        return CounterMessage.matchTo(
//                increment -> Command.pure(counter.withValue(counter.value() + 1)),
//                decrement -> Command.pure(counter.withValue(counter.value() + 1)),
//                setValue -> Command.pure(counter.withValue(setValue.value())),
//                dipslay -> Command.pure(counter)
//        );
        return null;
    }

    @Override
    public SpigotGUIView<CounterMessage> render(Counter counter) {
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