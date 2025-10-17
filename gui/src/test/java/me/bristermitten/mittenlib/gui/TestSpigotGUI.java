package me.bristermitten.mittenlib.gui;

import me.bristermitten.mittenlib.gui.spigot.InventoryButton;
import me.bristermitten.mittenlib.gui.spigot.SpigotGUI;
import me.bristermitten.mittenlib.gui.spigot.SpigotGUIView;
import me.bristermitten.mittenlib.gui.spigot.command.SpigotCommand;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class TestSpigotGUI extends SpigotGUI<Counter, CounterMessage, Object> {


    @Override
    public Counter init() {
        return Counter.create(0);
    }

    @Override
    public UpdateResult<Counter, CounterMessage, SpigotCommand<CounterMessage>> update(Counter counter, CounterMessage message) {
        return null;
    }

//    @Override
//    public Counter update(Counter counter, CounterMessage counterCommand) {
//        return counterCommand.matchTo(
//                increment -> Counter.create(counter.value() + 1),
//                decrement -> Counter.create(counter.value() - 1),
//                setValue -> Counter.create(setValue.value())
//        );
//    }

    @Override
    public SpigotGUIView<CounterMessage> render(Counter counter) {
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
