package me.bristermitten.mittenlib.annotations.integration;

import java.util.List;
import me.bristermitten.mittenlib.config.Config;

@Config
public interface CustomNameConfig {
    List<CustomNamedItemConfig> items();

    @Config(className = "CustomName")
    interface CustomNamedItemConfig {
        String name();

        int value();
    }
}
