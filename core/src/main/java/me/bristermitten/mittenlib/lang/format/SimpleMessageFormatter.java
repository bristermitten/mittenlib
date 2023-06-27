package me.bristermitten.mittenlib.lang.format;

import me.bristermitten.mittenlib.collections.Sets;
import me.bristermitten.mittenlib.lang.format.hook.FormattingHook;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Default implementation of {@link MessageFormatter} that uses {@link LegacyComponentSerializer} to create {@link Component}s.
 */
public class SimpleMessageFormatter extends AbstractMessageFormatter {
    @Inject
    SimpleMessageFormatter(Set<FormattingHook> hooks) {
        super(hooks);
    }

    @Override
    public @NotNull Component format(@NotNull String message, @Nullable OfflinePlayer player) {
        return LegacyComponentSerializer.legacySection().deserialize(preFormat(message, player));
    }

    @Override
    public @NotNull MessageFormatter withExtraHooks(@NotNull FormattingHook... hooks) {
        return new SimpleMessageFormatter(
                Sets.union(this.hooks, new HashSet<>(Arrays.asList(hooks)))
        );
    }
}
