package me.bristermitten.mittenlib.lang.format;

import me.bristermitten.mittenlib.lang.format.hook.FormattingHook;
import net.kyori.adventure.text.Component;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface MessageFormatter {
    @NotNull Component format(@NotNull String message, @Nullable OfflinePlayer player);

    @NotNull String preFormat(@NotNull String message, @Nullable OfflinePlayer player);

    @NotNull MessageFormatter withExtraHooks(@NotNull FormattingHook... hooks);

}
