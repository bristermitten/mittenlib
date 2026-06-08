package me.bristermitten.mittenlib.annotations.integration;

import me.bristermitten.mittenlib.config.Config;

@Config
public interface SimpleInterfaceConfig {
    String host();
    int port();
}