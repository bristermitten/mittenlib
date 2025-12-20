package me.bristermitten.mittenlib.gui.spigot;

import org.bukkit.inventory.ItemStack;

public class InventoryButton<Msg> {
    private final ItemStack itemStack;
    private final Msg message;

    public InventoryButton(ItemStack itemStack, Msg message) {
        this.itemStack = itemStack;
        this.message = message;
    }

    public Msg getMessage() {
        return message;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }
}
