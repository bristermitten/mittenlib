package me.bristermitten.mittenlib.lang.format;

import me.bristermitten.mittenlib.lang.format.hook.FormattingHook;
import net.kyori.adventure.text.Component;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
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
                .parse(preFormat(message, player));
    }

}
