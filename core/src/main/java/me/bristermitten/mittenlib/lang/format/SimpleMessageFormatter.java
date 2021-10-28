package me.bristermitten.mittenlib.lang.format;

import me.bristermitten.mittenlib.lang.format.hook.FormattingHook;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.util.Set;

public class SimpleMessageFormatter extends AbstractMessageFormatter {
    @Inject
    protected SimpleMessageFormatter(Set<FormattingHook> hooks) {
        super(hooks);
    }

    @Override
    public @NotNull Component format(@NotNull String message, @Nullable OfflinePlayer player) {
        return LegacyComponentSerializer.legacySection().deserialize(message);
    }
}
