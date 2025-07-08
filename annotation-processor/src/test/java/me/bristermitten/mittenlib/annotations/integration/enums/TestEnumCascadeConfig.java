package me.bristermitten.mittenlib.annotations.integration.enums;

import me.bristermitten.mittenlib.config.Config;
import me.bristermitten.mittenlib.config.EnumParsingScheme;
import me.bristermitten.mittenlib.config.EnumParsingSchemes;

@Config
@EnumParsingScheme(EnumParsingSchemes.CASE_INSENSITIVE)
public interface TestEnumCascadeConfig {
    TestEnum testEnum();

    TestEnum testEnumInexact();
}
