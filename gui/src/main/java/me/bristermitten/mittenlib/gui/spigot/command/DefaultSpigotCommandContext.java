package me.bristermitten.mittenlib.gui.spigot.command;

import me.bristermitten.mittenlib.gui.spigot.InventoryStorage;
import me.bristermitten.mittenlib.gui.spigot.SpigotGUIView;
import me.bristermitten.mittenlib.gui.spigot.SpigotInventoryViewer;
import org.bukkit.entity.Player;

import java.util.Optional;

public class DefaultSpigotCommandContext<Msg> implements SpigotCommandContext<Msg> {
    private final Player player;
    private final SpigotInventoryViewer<Msg> viewer;
    private final Optional<SpigotGUIView<Msg>> currentView;
    private final InventoryStorage storage;

    public DefaultSpigotCommandContext(Player player,
                                       SpigotInventoryViewer<Msg> viewer,
                                       Optional<SpigotGUIView<Msg>> currentView,
                                       InventoryStorage storage) {
        this.player = player;
        this.viewer = viewer;
        this.currentView = currentView;
        this.storage = storage;
    }

    @Override
    public Player player() {
        return player;
    }

    @Override
    public SpigotInventoryViewer<Msg> viewer() {
        return viewer;
    }

    @Override
    public Optional<SpigotGUIView<Msg>> currentView() {
        return currentView;
    }

    @Override
    public void open(SpigotGUIView<Msg> view) {
        view.display(viewer);
        view.storeInInventoryStorage(storage);
    }
}

