package me.bristermitten.mittenlib.docs.tutorial;

import me.bristermitten.mittenlib.config.Config;
import me.bristermitten.mittenlib.config.Source;
import me.bristermitten.mittenlib.config.validation.Min;
import me.bristermitten.mittenlib.config.validation.NotBlank;

import java.util.List;

@Config
@Source("config.yml")
public interface AdvancedConfig {
    @NotBlank
    String host();

    @Min(1)
    default int port() {
        return 3306;
    }

    default List<String> motd() {
        return List.of("Welcome to the server!");
    }

    DatabaseConfig database();
    
    @Config
    interface DatabaseConfig {
        String username();
        String password();
    }
}
