package me.bristermitten.mittenlib.lang;

import static me.bristermitten.mittenlib.util.Cast.safeCast;

import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;

import javax.inject.Inject;

import me.bristermitten.mittenlib.lang.format.MessageFormatter;

public class LangService {

    private final MessageFormatter formatter;
    private final BukkitAudiences audiences;


    @Inject
    public LangService(MessageFormatter formatter, BukkitAudiences audiences) {
        this.formatter = formatter;
        this.audiences = audiences;
    }


    public void send(@NotNull CommandSender receiver, @NotNull LangMessage langMessage) {
        send(receiver, langMessage, Collections.emptyMap(), null);
    }

    public void send(@NotNull CommandSender receiver, @NotNull LangMessage langMessage, @NotNull Map<String, Object> placeholders) {
        send(receiver, langMessage, placeholders, null);
    }

    public void send(@NotNull CommandSender receiver, @NotNull LangMessage langMessage, @Nullable String messagePrefix) {
        send(receiver, langMessage, Collections.emptyMap(), messagePrefix);
    }

    public void send(@NotNull CommandSender receiver, @NotNull LangMessage langMessage, @NotNull Map<String, Object> placeholders, @Nullable String messagePrefix) {
        if (langMessage instanceof CompoundLangMessage) {
            CompoundLangMessage compound = (CompoundLangMessage) langMessage;
            for (LangMessage message : compound.getComponents()) {
                send(receiver, message, placeholders, messagePrefix);
            }
        }
        UnaryOperator<String> applyPlaceholders = str -> {
            for (Map.Entry<String, Object> entry : placeholders.entrySet()) {
                str = str.replace(entry.getKey(), entry.getValue().toString());
            }
            return str;
        };

        if (langMessage.getMessage() != null) {
            String message = langMessage.getMessage();
            if (messagePrefix != null) message = messagePrefix + message;
            final String replaced = applyPlaceholders.apply(message);
            sendMessage(receiver, replaced);
        }

        if (langMessage.getTitle() != null || langMessage.getSubtitle() != null) {
            final String title = Optional.ofNullable(langMessage.getTitle())
                    .map(applyPlaceholders)
                    .orElse(null);
            final String subtitle = Optional.ofNullable(langMessage.getSubtitle())
                    .map(applyPlaceholders)
                    .orElse("");

            sendTitle(receiver, title, subtitle);
        }
        if (langMessage.getActionBar() != null) {
            sendActionBar(receiver, applyPlaceholders.apply(langMessage.getActionBar()));
        }

        final LangMessage.SoundConfig sound = langMessage.getSound();
        if (sound != null && receiver instanceof Player) {
            Player player = (Player) receiver;
            player.playSound(player.getLocation(), sound.getSound(), sound.getVolume(), sound.getPitch());
        }
    }

    public void sendMessage(CommandSender receiver, String message) {
        audiences.sender(receiver).sendMessage(getFormattedComponent(receiver, message));
    }

    private Component getFormattedComponent(CommandSender receiver, String message) {
        return formatter.format(message, safeCast(receiver, OfflinePlayer.class));
    }

    public void sendActionBar(CommandSender receiver, String message) {
        audiences.sender(receiver).sendActionBar(getFormattedComponent(receiver, message));
    }

    public void sendTitle(CommandSender receiver, String title, String subtitle) {
        audiences.sender(receiver).showTitle(Title.title(
                getFormattedComponent(receiver, title),
                getFormattedComponent(receiver, subtitle)
        ));
    }

}
