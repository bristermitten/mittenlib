package me.bristermitten.mittenlib.gui.spigot;

import me.bristermitten.mittenlib.codegen.RecordSpec;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.ItemStack;

import java.util.OptionalInt;

/**
 * Input about a click event in a {@link SpigotGUI}.
 * Used to generate messages from {@link InventoryButton}s.
 */
@RecordSpec
public interface ClickInputSpec {

    ClickType type();

    InventoryAction action();

    OptionalInt hotbarKey();

    ItemStack cursor();

    default boolean hasCursor(Material mat) {
        return cursor() != null && cursor().getType() == mat;
    }
}
