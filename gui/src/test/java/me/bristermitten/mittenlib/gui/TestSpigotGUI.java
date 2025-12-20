package me.bristermitten.mittenlib.gui;

import me.bristermitten.mittenlib.gui.spigot.InventoryButton;
import me.bristermitten.mittenlib.gui.spigot.SpigotGUI;
import me.bristermitten.mittenlib.gui.spigot.SpigotGUIView;
import me.bristermitten.mittenlib.gui.spigot.command.SpigotCommand;
import me.bristermitten.mittenlib.gui.spigot.command.SpigotCommandContext;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class TestSpigotGUI extends SpigotGUI<Counter, CounterMessage> {


    @Override
    public @NotNull Counter init() {
        return Counter.create(0);
    }

    @Override
    public @NotNull UpdateResult<Counter, CounterMessage, SpigotCommandContext<CounterMessage>, SpigotCommand<CounterMessage>> update(Counter counter, CounterMessage message) {
        return message.matchTo(
                increment -> UpdateResult.pure(counter.withValue(counter.value() + 1)),
                decrement -> UpdateResult.pure(counter.withValue(counter.value() - 1)),
                setValue -> UpdateResult.pure(counter.withValue(setValue.value())),
                ask -> UpdateResult.pure(counter) // TODO
        );
    }


    @Override
    public @NotNull SpigotGUIView<CounterMessage> render(Counter counter) {
        return SpigotGUIView.<CounterMessage>create(9, "Counter GUI")
                .withButton(0, new InventoryButton<>(
                        new ItemStack(Material.STONE),
                        CounterMessage.Increment()
                ))
                .withButton(1, new InventoryButton<>(
                        new ItemStack(Material.DIRT),
                        CounterMessage.Decrement()
                ))
                .withButton(2, new InventoryButton<>(
                        new ItemStack(Material.GOLD_INGOT),
                        CounterMessage.Set(42) // needs better parsing
                ));
    }


}
