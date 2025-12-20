package me.bristermitten.mittenlib.gui.spigot.command;

import me.bristermitten.mittenlib.gui.command.CommandContext;
import me.bristermitten.mittenlib.gui.spigot.SpigotGUIView;
import me.bristermitten.mittenlib.gui.spigot.SpigotInventoryViewer;
import org.bukkit.entity.Player;

import java.util.Optional;

public interface SpigotCommandContext<Msg> extends CommandContext {
    Player player();

    SpigotInventoryViewer<Msg> viewer();

    Optional<SpigotGUIView<Msg>> currentView();

    void open(SpigotGUIView<Msg> view);
}
