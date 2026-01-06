package me.bristermitten.mittenlib.gui.spigot;

import me.bristermitten.mittenlib.util.lambda.PureFunction;
import org.bukkit.inventory.ItemStack;

/**
 * A button in a Spigot GUI Inventory.
 *
 * @param <Msg> The message type that this button produces
 */
public class InventoryButton<Msg> {
    private final ItemStack itemStack;

    private final PureFunction<ClickInput, @Nullable Msg> messageFunction;

    public InventoryButton(ItemStack itemStack, PureFunction<ClickInput, @Nullable Msg> messageFunction) {
        this.itemStack = itemStack;
        this.messageFunction = messageFunction;
    }

    /**
     * @param itemStack The {@link ItemStack} to render the button as.
     * @param message   The pure function producing a {@link Msg} from a {@link ClickInput}.
     *                  This function is expected to be pure, and undefined behaviour may occur if it is not.
     *                  The function may return null, which is treated as no message, i.e. "do nothing".
     */
    public InventoryButton(ItemStack itemStack, @Nullable Msg message) {
        this.itemStack = itemStack;
        this.messageFunction = clickInput -> message;
    }


    /**
     * @return The pure function producing a {@link Msg} from a {@link ClickInput}
     */
    public PureFunction<ClickInput, @Nullable Msg> getMessageFunction() {
        return messageFunction;
    }

    /**
     * @return The {@link ItemStack} to render the button as.
     */
    public ItemStack getItemStack() {
        return itemStack;
    }
}
