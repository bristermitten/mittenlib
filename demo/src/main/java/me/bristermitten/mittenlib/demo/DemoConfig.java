package me.bristermitten.mittenlib.demo;

import me.bristermitten.mittenlib.config.Config;
import me.bristermitten.mittenlib.config.Source;
import me.bristermitten.mittenlib.config.validation.Positive;

@Config
@Source("config.yml")
public interface DemoConfig {
    @Positive default int port() {
        return 25565;
    }
}
