package me.bristermitten.mittenlib.gui.spigot;

import me.bristermitten.mittenlib.util.lambda.PureFunction;
import org.bukkit.inventory.ItemStack;

public class InventoryButton<Msg> {
    private final ItemStack itemStack;

    private final PureFunction<ClickInput, Msg> messageFunction;

    public InventoryButton(ItemStack itemStack, PureFunction<ClickInput, Msg> messageFunction) {
        this.itemStack = itemStack;
        this.messageFunction = messageFunction;
    }

    public InventoryButton(ItemStack itemStack, Msg message) {
        this.itemStack = itemStack;
        this.messageFunction = clickInput -> message;
    }


    public PureFunction<ClickInput, Msg> getMessageFunction() {
        return messageFunction;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }
}
