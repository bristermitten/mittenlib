package me.bristermitten.mittenlib.lang.format;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface AbstractMiniMessageFactory {
    @NotNull MiniMessage create(@Nullable OfflinePlayer player);
}
