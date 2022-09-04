package me.bristermitten.mittenlib.lang.format.hook;

import me.bristermitten.mittenlib.util.Version;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This is a truly outrageous hack
 * Essentially, when a papi expansion returns a hex color, it doesn't get parsed properly by MiniMessage
 * and instead it just takes it to the nearest legacy color
 * This class tries to parse the §x§a... hex format that {@link net.md_5.bungee.api.ChatColor} uses
 * and turns it into a format that MiniMessage can recognise
 */
public class HexColorFixerHook implements FormattingHook {
    private static final Pattern HEX_PATTERN = Pattern.compile("§x((§[0-9a-f]){6})");

    @Override
    public boolean shouldRegister() {
        return Version.getServerVersion().isNewerThan(Version.VER_1_16);
    }

    @Override
    public @NotNull String format(@NotNull String message, @Nullable OfflinePlayer player) {
        final Matcher matcher = HEX_PATTERN.matcher(message);


        final StringBuffer stringBuilder = new StringBuffer();
        while (matcher.find()) {
            final String replacement = "<#" + matcher.group(1)
                    .replace("" + ChatColor.COLOR_CHAR, "") + ">";
            matcher.appendReplacement(stringBuilder, replacement);
        }
        matcher.appendTail(stringBuilder);

        return stringBuilder.toString();
    }
}
