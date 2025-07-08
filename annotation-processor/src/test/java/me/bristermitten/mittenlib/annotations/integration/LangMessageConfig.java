package me.bristermitten.mittenlib.annotations.integration;

import me.bristermitten.mittenlib.config.Config;
import me.bristermitten.mittenlib.lang.LangMessage;

@Config
public interface LangMessageConfig {
    LangMessage hello();
}
