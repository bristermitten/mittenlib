package me.bristermitten.mittenlib.annotations.integration.enums;

import me.bristermitten.mittenlib.config.Config;
import me.bristermitten.mittenlib.config.EnumParsingScheme;
import me.bristermitten.mittenlib.config.EnumParsingSchemes;

@Config
public interface TestEnumConfig {
    @EnumParsingScheme(EnumParsingSchemes.EXACT_MATCH)
    TestEnum testEnum();

    @EnumParsingScheme(EnumParsingSchemes.CASE_INSENSITIVE)
    TestEnum testEnumInexact();
}
