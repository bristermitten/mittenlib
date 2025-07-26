package me.bristermitten.mittenlib.gui.spigot;

import org.bukkit.inventory.Inventory;

import javax.inject.Singleton;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

@Singleton
public class InventoryStorage {
    private final Map<Inventory, SpigotGUIView<?>> inventoryMap = new WeakHashMap<>();

    public void storeInventory(Inventory inventory, SpigotGUIView<?> view) {
        inventoryMap.put(inventory, view);
    }

    public Optional<SpigotGUIView<?>> getView(Inventory inventory) {
        return Optional.ofNullable(inventoryMap.get(inventory));
    }

    public void removeInventory(Inventory inventory) {
        inventoryMap.remove(inventory);
    }

    public void clear() {
        inventoryMap.clear();
    }
}
