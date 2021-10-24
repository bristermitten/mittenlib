package me.bristermitten.mittenlib.lang;

import org.bukkit.Sound;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class LangMessage {
    private final @Nullable String message;
    private final @Nullable String title;
    private final @Nullable String subtitle;
    private final @Nullable String actionBar;
    private final @Nullable SoundConfig sound;

    public LangMessage(@Nullable String message, @Nullable String title, @Nullable String subtitle, @Nullable String actionBar, @Nullable SoundConfig sound) {
        this.message = message;
        this.title = title;
        this.subtitle = subtitle;
        this.actionBar = actionBar;
        this.sound = sound;
    }

    public @Nullable String getMessage() {
        return message;
    }

    public @Nullable String getTitle() {
        return title;
    }

    public @Nullable String getSubtitle() {
        return subtitle;
    }

    public @Nullable String getActionBar() {
        return actionBar;
    }

    public @Nullable SoundConfig getSound() {
        return sound;
    }

    public static class SoundConfig {
        private final @NotNull Sound sound;
        private final float volume;
        private final float pitch;

        public SoundConfig(@NotNull Sound sound, float volume, float pitch) {
            this.sound = sound;
            this.volume = volume;
            this.pitch = pitch;
        }

        public @NotNull Sound getSound() {
            return sound;
        }

        public float getVolume() {
            return volume;
        }

        public float getPitch() {
            return pitch;
        }
    }
}
