package me.bristermitten.mittenlib.annotations.integration;

import io.toolisticon.cute.PassIn;
import me.bristermitten.mittenlib.config.Config;

@Config
@PassIn
public interface AtomicConfig {
    String name();
}
