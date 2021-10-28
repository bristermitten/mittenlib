package me.bristermitten.mittenlib.lang.format;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DefaultMiniMessageFactory implements AbstractMiniMessageFactory {
    @Override
    public @NotNull MiniMessage create(@Nullable OfflinePlayer player) {
        return MiniMessage.builder()
                .build();
    }
}
