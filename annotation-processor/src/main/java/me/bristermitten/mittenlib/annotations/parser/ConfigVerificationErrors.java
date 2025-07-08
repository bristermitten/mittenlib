package me.bristermitten.mittenlib.annotations.parser;

import io.toolisticon.aptk.tools.corematcher.PlainValidationMessage;
import io.toolisticon.aptk.tools.corematcher.ValidationMessage;

public class ConfigVerificationErrors {
    public static final ValidationMessage UNION_ALTERNATIVE_NOT_EXTENDING_UNION =
            PlainValidationMessage.create("UNION_ALTERNATIVE_NOT_EXTENDING_UNION",
                    "Alternative in union ${0} MUST extend the union type when the union type has properties defined!"
            );

    public static final ValidationMessage ENUM_PARSING_SCHEME_NOT_ENUM =
            PlainValidationMessage.create("ENUM_PARSING_SCHEME_NOT_ENUM",
                    "This property's type is not an enum, so the @EnumParsingScheme annotation will have no effect."
            );


    public static final ValidationMessage CUSTOM_DESERIALIZER_INVALID_STATIC_METHOD_SIGNATURE =
            PlainValidationMessage.create("CUSTOM_DESERIALIZER_INVALID_STATIC_METHOD_SIGNATURE",
                    "Custom deserializer method must be static and be of the signature Result<${0}> deserialize(DeserializationContext)"
            );
}
