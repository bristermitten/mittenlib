package me.bristermitten.mittenlib.lang.format.hook;

import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LegacyCodeUpdaterHook implements FormattingHook {
    private static final Pattern LEGACY_CODE_PATTERN = Pattern.compile("§[\\da-fk-or]");

    @Override
    public boolean shouldRegister() {
        return true;
    }

    @Override
    public String format(String message, @Nullable OfflinePlayer player) {
        Matcher matcher = LEGACY_CODE_PATTERN.matcher(message);
        StringBuffer builder = new StringBuffer();
        while (matcher.find()) {
            char codeChar = matcher.group(0).charAt(1);
            ChatColor code = ChatColor.getByChar(codeChar);
            String replacement = "<" + code.name().toLowerCase() + ">";
            matcher.appendReplacement(builder, replacement);
        }
        matcher.appendTail(builder);
        return builder.toString();
    }
}
