package me.bristermitten.mittenlib.gui;

import me.bristermitten.mittenlib.gui.spigot.InventoryButton;
import me.bristermitten.mittenlib.gui.spigot.SpigotGUI;
import me.bristermitten.mittenlib.gui.spigot.SpigotGUIView;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class TestSpigotGUI extends SpigotGUI<Counter, CounterCommand> {


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
        return SpigotGUIView.<CounterCommand>create(9, "Counter GUI")
                .withButton(0, new InventoryButton<>(
                        new ItemStack(Material.STONE),
                        CounterCommand.Increment()
                ))
                .withButton(1, new InventoryButton<>(
                        new ItemStack(Material.DIRT),
                        CounterCommand.Decrement()
                ))
                .withButton(2, new InventoryButton<>(
                        new ItemStack(Material.GOLD_INGOT),
                        CounterCommand.Set(42) // needs better parsing
                ));
    }


}
