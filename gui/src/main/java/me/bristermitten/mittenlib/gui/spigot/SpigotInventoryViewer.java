package me.bristermitten.mittenlib.gui.spigot;

import me.bristermitten.mittenlib.gui.view.InventoryViewer;
import org.bukkit.entity.Player;

public class SpigotInventoryViewer<C> implements InventoryViewer<C, SpigotGUIView<C>> {
    private final Player player;

    public SpigotInventoryViewer(Player player) {
        this.player = player;
    }

    @Override
    public void display(SpigotGUIView<C> view) {
        player.openInventory(view.inventory);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof SpigotInventoryViewer)) return false;
        SpigotInventoryViewer<?> that = (SpigotInventoryViewer<?>) obj;
        return player.equals(that.player);
    }

    @Override
    public int hashCode() {
        return player.hashCode();
    }
}
