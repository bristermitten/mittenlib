package me.bristermitten.mittenlib.annotations.integration;

import me.bristermitten.mittenlib.config.Config;
import me.bristermitten.mittenlib.config.ConfigTransient;

@Config
public interface ConfigTransientConfig {

    class ICantBeSerializedHeheh {}

    @ConfigTransient
    default ICantBeSerializedHeheh ohno() {
        return null;
    }

    int iCanBeSerialized();
}
