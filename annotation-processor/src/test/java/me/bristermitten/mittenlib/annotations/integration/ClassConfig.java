package me.bristermitten.mittenlib.annotations.integration;

import java.util.List;
import me.bristermitten.mittenlib.config.Config;
import me.bristermitten.mittenlib.config.generate.GenerateToString;
import me.bristermitten.mittenlib.config.names.ConfigName;
import org.jspecify.annotations.Nullable;

@Config
@GenerateToString
public class ClassConfig {
    @ConfigName("thing-name")
    String name;

    int age;

    int defaultValue = 1;

    List<InterfaceConfig> children;

    @Nullable ChildConfig child;

    @Config
    interface ChildConfig {
        String id();
    }
}
