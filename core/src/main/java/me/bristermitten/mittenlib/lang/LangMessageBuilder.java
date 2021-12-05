package me.bristermitten.mittenlib.lang;

import org.jetbrains.annotations.Nullable;

public class LangMessageBuilder {
    private @Nullable String message;
    private @Nullable String title;
    private @Nullable String subtitle;
    private @Nullable String actionBar;
    private @Nullable LangMessage.SoundConfig sound;

    public LangMessageBuilder setMessage(@Nullable String message) {
        this.message = message;
        return this;
    }

    public LangMessageBuilder setTitle(@Nullable String title) {
        this.title = title;
        return this;
    }

    public LangMessageBuilder setSubtitle(@Nullable String subtitle) {
        this.subtitle = subtitle;
        return this;
    }

    public LangMessageBuilder setActionBar(@Nullable String actionBar) {
        this.actionBar = actionBar;
        return this;
    }

    public LangMessageBuilder setSound(@Nullable LangMessage.SoundConfig sound) {
        this.sound = sound;
        return this;
    }

    public LangMessage createLangMessage() {
        return new LangMessage(message, title, subtitle, actionBar, sound);
    }
}
