package me.bristermitten.mittenlib.gui.factory;

import me.bristermitten.mittenlib.gui.spigot.InventoryButton;
import me.bristermitten.mittenlib.gui.spigot.SpigotGUIView;
import org.bukkit.inventory.ItemStack;

/**
 * Factory interface for creating GUI components.
 * Provides a clean abstraction for component creation without static methods.
 */
public interface MinecraftGUIFactory {

    /**
     * Creates a new SpigotGUIView with the specified size and title.
     *
     * @param size      the inventory size (must be a multiple of 9)
     * @param title     the inventory title
     * @param <Command> the command type
     * @return a new SpigotGUIView instance
     */
    <Command> SpigotGUIView<Command> createSpigotView(int size, String title);

    /**
     * Creates a new InventoryButton with the specified item and command.
     *
     * @param itemStack the item stack to display
     * @param command   the command to execute when clicked
     * @param <Command> the command type
     * @return a new InventoryButton instance
     */
    <Command> InventoryButton<Command> createButton(ItemStack itemStack, Command command);

    /**
     * Creates a new InventoryButton with the specified item, command, and display name.
     *
     * @param itemStack   the base item stack
     * @param command     the command to execute when clicked
     * @param displayName the display name for the item
     * @param <Command>   the command type
     * @return a new InventoryButton instance with custom display name
     */
    <Command> InventoryButton<Command> createButton(ItemStack itemStack, Command command, String displayName);
}