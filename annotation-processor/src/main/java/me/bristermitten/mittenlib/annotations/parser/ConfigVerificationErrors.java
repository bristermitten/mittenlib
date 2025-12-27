package me.bristermitten.mittenlib.annotations.parser;

import io.toolisticon.aptk.tools.corematcher.ValidationMessage;
import me.bristermitten.mittenlib.annotations.CompileTimeErrors;

/**
 * Compile-time validation error messages for configuration processing.
 * @deprecated Use {@link CompileTimeErrors} instead. This class is maintained for backward compatibility.
 */
@Deprecated
public class ConfigVerificationErrors {
    public static final ValidationMessage UNION_ALTERNATIVE_NOT_EXTENDING_UNION = 
            CompileTimeErrors.UNION_ALTERNATIVE_NOT_EXTENDING_UNION;

    public static final ValidationMessage ENUM_PARSING_SCHEME_NOT_ENUM = 
            CompileTimeErrors.ENUM_PARSING_SCHEME_NOT_ENUM;

    public static final ValidationMessage CUSTOM_DESERIALIZER_INVALID_STATIC_METHOD_SIGNATURE = 
            CompileTimeErrors.CUSTOM_DESERIALIZER_INVALID_SIGNATURE;
}
