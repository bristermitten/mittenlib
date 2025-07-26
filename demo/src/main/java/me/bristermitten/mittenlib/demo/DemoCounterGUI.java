package me.bristermitten.mittenlib.demo;

import com.google.inject.Inject;
import me.bristermitten.mittenlib.gui.factory.MinecraftGUIFactory;
import me.bristermitten.mittenlib.gui.spigot.SpigotGUI;
import me.bristermitten.mittenlib.gui.spigot.SpigotGUIView;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Improved counter GUI that uses dependency injection instead of static methods.
 * Demonstrates the new Elm-like architecture with proper Guice integration.
 */
public class DemoCounterGUI extends SpigotGUI<Counter, CounterCommand> {

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
    public Counter update(Counter counter, CounterCommand counterCommand) {
        return counterCommand.matchTo(
                increment -> Counter.create(counter.value() + 1),
                decrement -> Counter.create(counter.value() - 1),
                setValue -> Counter.create(setValue.value())
        );
    }

    @Override
    public SpigotGUIView<CounterCommand> render(Counter counter) {
        return minecraftGuiFactory.<CounterCommand>createSpigotView(27, "Count: " + counter.value())
                .withButton(10, minecraftGuiFactory.createButton(
                        new ItemStack(Material.EMERALD),
                        CounterCommand.Increment(),
                        "§a+1 (Click to increment)"
                ))
                .withButton(13, minecraftGuiFactory.createButton(
                        new ItemStack(Material.GOLD_INGOT),
                        CounterCommand.Set(0),
                        "§eReset (Click to reset to 0)"
                ))
                .withButton(16, minecraftGuiFactory.createButton(
                        new ItemStack(Material.REDSTONE),
                        CounterCommand.Decrement(),
                        "§c-1 (Click to decrement)"
                ))
                .withButton(22, minecraftGuiFactory.createButton(
                        new ItemStack(Material.DIAMOND),
                        CounterCommand.Set(100),
                        "§b+100 (Click to set to 100)"
                ));
    }
}