package me.bristermitten.mittenlib.annotations.integration;

import io.toolisticon.cute.PassIn;
import me.bristermitten.mittenlib.config.Config;
import me.bristermitten.mittenlib.config.generate.GenerateToString;
import me.bristermitten.mittenlib.config.names.ConfigName;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Config
@GenerateToString
@PassIn
public interface InterfaceConfig {
    @ConfigName("thing-name")
    String name();

    int age();

    List<InterfaceConfig> children();

    @Nullable ChildConfig child();

    @Config
    interface ChildConfig {
        String id();
    }
}