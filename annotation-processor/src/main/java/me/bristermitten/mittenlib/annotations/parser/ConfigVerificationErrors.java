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

    public static final ValidationMessage CLASS_DTO_MISSING_NO_ARG_CONSTRUCTOR =
            PlainValidationMessage.create("CLASS_DTO_MISSING_NO_ARG_CONSTRUCTOR",
                    "Class DTO ${0} has fields with default values, but is missing an accessible (non-private) zero-arguments constructor. Please define an accessible zero-arguments constructor (package-private, protected, or public) so MittenLib can read the default values."
            );

    public static final ValidationMessage CONSTRAINT_TYPE_MISMATCH =
            PlainValidationMessage.create("CONSTRAINT_TYPE_MISMATCH",
                    "Constraint annotation ${0} cannot be applied to type ${1}. Expected a ${2}."
            );

    public static final ValidationMessage SERIALIZATION_NOT_SUPPORTED =
            PlainValidationMessage.create("SERIALIZATION_NOT_SUPPORTED",
                    "Serialization is required for this config, but it contains properties that cannot be serialized: ${0}"
            );

    public static final ValidationMessage SERIALIZATION_NOT_SUPPORTED_WARNING =
            PlainValidationMessage.create("SERIALIZATION_NOT_SUPPORTED_WARNING",
                    "This config contains properties that cannot be serialized: ${0}. Attempting to serialize this config may result in runtime exceptions. If you want to require serialization, set requireSerialization to true."
            );
}
