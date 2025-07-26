package me.bristermitten.mittenlib.gui.spigot;

import org.bukkit.inventory.ItemStack;

public class InventoryButton<Command> {
    private final ItemStack itemStack;
    private final Command command;

    public InventoryButton(ItemStack itemStack, Command command) {
        this.itemStack = itemStack;
        this.command = command;
    }

    public Command getCommand() {
        return command;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }
}
