package me.bristermitten.mittenlib.gui.factory;

import com.google.inject.Inject;
import me.bristermitten.mittenlib.collections.Maps;
import me.bristermitten.mittenlib.gui.spigot.InventoryButton;
import me.bristermitten.mittenlib.gui.spigot.SpigotGUIView;
import me.bristermitten.mittenlib.lang.format.MessageFormatter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Spigot implementation of GUIFactory for creating GUI components.
 * Replaces static factory methods with dependency-injected instances.
 */
public class SpigotMinecraftGUIFactory implements MinecraftGUIFactory {

    private final MessageFormatter messageFormatter;

    @Inject
    public SpigotMinecraftGUIFactory(MessageFormatter messageFormatter) {
        this.messageFormatter = messageFormatter;

    }

    @Override
    public <Command> SpigotGUIView<Command> createSpigotView(int size, String title) {
        if (size <= 0 || size % 9 != 0) {
            throw new IllegalArgumentException("Inventory size must be a positive multiple of 9");
        }
        if (size > 54) {
            throw new IllegalArgumentException("Inventory size cannot exceed 54 slots");
        }
        Component formattedTitle = messageFormatter.format(title, null);

        return new SpigotGUIView<>(size, LegacyComponentSerializer.legacySection().serialize(formattedTitle), Maps.of());
    }

    @Override
    public <Command> InventoryButton<Command> createButton(ItemStack itemStack, Command command) {
        if (itemStack == null) {
            throw new IllegalArgumentException("ItemStack cannot be null");
        }
        if (command == null) {
            throw new IllegalArgumentException("Command cannot be null");
        }

        return new InventoryButton<>(itemStack.clone(), command);
    }

    @Override
    public <Command> InventoryButton<Command> createButton(ItemStack itemStack, Command command, String displayName) {
        if (itemStack == null) {
            throw new IllegalArgumentException("ItemStack cannot be null");
        }
        if (command == null) {
            throw new IllegalArgumentException("Command cannot be null");
        }
        if (displayName == null) {
            throw new IllegalArgumentException("Display name cannot be null");
        }

        ItemStack customItem = itemStack.clone();
        ItemMeta meta = customItem.getItemMeta();
        if (meta != null) {
            Component formattedName = messageFormatter.format(displayName, null);
            meta.setDisplayName(LegacyComponentSerializer.legacySection().serialize(formattedName));
            customItem.setItemMeta(meta);
        }

        return new InventoryButton<>(customItem, command);
    }
}