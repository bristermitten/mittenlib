package me.bristermitten.mittenlib.annotations.integration;

import me.bristermitten.mittenlib.config.Config;

import java.util.List;

@Config
public interface CustomNameConfig {
    List<CustomNamedItemConfig> items();

    @Config(className = "CustomName")
    interface CustomNamedItemConfig {
        String name();

        int value();
    }
}
