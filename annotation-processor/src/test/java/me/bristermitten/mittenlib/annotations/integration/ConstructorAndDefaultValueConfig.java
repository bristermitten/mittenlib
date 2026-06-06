package me.bristermitten.mittenlib.annotations.integration;

import me.bristermitten.mittenlib.config.Config;

@Config
public class ConstructorAndDefaultValueConfig {
    public int x = 3;
    public int y;

    ConstructorAndDefaultValueConfig() {}

    public ConstructorAndDefaultValueConfig(int y) {
        this.y = y;
    }
}
