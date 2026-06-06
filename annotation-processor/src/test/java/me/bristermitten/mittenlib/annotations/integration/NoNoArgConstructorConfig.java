package me.bristermitten.mittenlib.annotations.integration;

import me.bristermitten.mittenlib.config.Config;

@Config
public class NoNoArgConstructorConfig {
    public int id;
    public String name;

    public NoNoArgConstructorConfig(int id, String name) {
        this.id = id;
        this.name = name;
    }
}
