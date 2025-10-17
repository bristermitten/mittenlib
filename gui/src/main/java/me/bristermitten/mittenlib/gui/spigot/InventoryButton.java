package me.bristermitten.mittenlib.gui.spigot;

import org.bukkit.inventory.ItemStack;

public class InventoryButton<Msg> {
    private final ItemStack itemStack;
    private final Msg command;

    public InventoryButton(ItemStack itemStack, Msg command) {
        this.itemStack = itemStack;
        this.command = command;
    }

    public Msg getMessage() {
        return command;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }
}
