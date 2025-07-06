package me.bristermitten.mittenlib.annotations.integration;

import me.bristermitten.mittenlib.config.Config;

@Config
public interface DeeplyNestedIntegrationConfig {
    String a();

    @Config
    interface Child1Config {
        @Config
        interface Child2Config {
        }
    }
}
