package me.bristermitten.mittenlib.lang.format;

import me.bristermitten.mittenlib.collections.Sets;
import me.bristermitten.mittenlib.lang.format.hook.FormattingHook;
import net.kyori.adventure.text.Component;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MiniMessageFormatter extends AbstractMessageFormatter {
    private final AbstractMiniMessageFactory miniMessageFactory;

    @Inject
    public MiniMessageFormatter(Set<FormattingHook> hooks, AbstractMiniMessageFactory miniMessageFactory) {
        super(hooks);
        this.miniMessageFactory = miniMessageFactory;
    }

    @Override
    public @NotNull Component format(@NotNull String message, @Nullable OfflinePlayer player) {
        return miniMessageFactory.create(player)
                .deserialize(preFormat(message, player));
    }

    @Override
    public @NotNull MessageFormatter withExtraHooks(@NotNull FormattingHook... hooks) {
        return new MiniMessageFormatter(
                Sets.concat(this.hooks, new HashSet<>(Arrays.asList(hooks))),
                miniMessageFactory
        );
    }

}
