package me.bristermitten.mittenlib.gui.spigot;

import me.bristermitten.mittenlib.collections.MLImmutableMap;
import me.bristermitten.mittenlib.collections.Maps;
import me.bristermitten.mittenlib.gui.view.View;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Map;
import java.util.Optional;

public class SpigotGUIView<Command> implements View<Command, SpigotGUIView<Command>, SpigotInventoryViewer<Command>> {
    private final int size;
    private final String title;

    private final @Unmodifiable MLImmutableMap<Integer, InventoryButton<Command>> buttons;
    Inventory inventory;

    public SpigotGUIView(int size, String title, @Unmodifiable Map<Integer, InventoryButton<Command>> buttons) {
        this.size = size;
        this.title = title;
        this.buttons = Maps.of(buttons.entrySet());
    }

    public static <Command> SpigotGUIView<Command> create(int size, String title) {
        return new SpigotGUIView<>(size, title, Maps.of());
    }

    public SpigotGUIView<Command> withButton(int slot, InventoryButton<Command> button) {
        return new SpigotGUIView<>(size, title, buttons.plus(slot, button));
    }

    public Optional<InventoryButton<Command>> getButton(int slot) {
        return Optional.ofNullable(buttons.get(slot));
    }

    @Override
    public void display(SpigotInventoryViewer<Command> inventoryViewer) {
        Inventory inventory = Bukkit.createInventory(
                null,
                size,
                title
        );
        buttons.forEach((slot, button) -> {
            inventory.setItem(slot, button.getItemStack());
        });
        this.inventory = inventory;

        inventoryViewer.display(this);
    }

    public void storeInInventoryStorage(InventoryStorage storage) {
        if (inventory != null) {
            storage.storeInventory(inventory, this);
        }
    }

    @Override
    public Command waitForCommand() {
        return null;
    }
}
