package me.bristermitten.mittenlib.annotations;

import io.toolisticon.aptk.tools.corematcher.PlainValidationMessage;
import io.toolisticon.aptk.tools.corematcher.ValidationMessage;

/**
 * Centralized compile-time error messages for configuration processing.
 * These messages help developers identify and fix issues in their configuration DTO classes during compilation.
 */
public class CompileTimeErrors {

    // ==================== Union Errors ====================
    
    public static final ValidationMessage UNION_ALTERNATIVE_NOT_EXTENDING_UNION =
            PlainValidationMessage.create("UNION_ALTERNATIVE_NOT_EXTENDING_UNION",
                    "Union alternative '${1}' must extend union type '${0}' when properties are defined in the union base type.");

    // ==================== Enum Errors ====================
    
    public static final ValidationMessage ENUM_PARSING_SCHEME_NOT_ENUM =
            PlainValidationMessage.create("ENUM_PARSING_SCHEME_NOT_ENUM",
                    "@EnumParsingScheme can only be applied to enum properties. Remove the annotation or change the property type to an enum.");

    // ==================== Custom Deserializer Errors ====================
    
    public static final ValidationMessage CUSTOM_DESERIALIZER_INVALID_SIGNATURE =
            PlainValidationMessage.create("CUSTOM_DESERIALIZER_INVALID_SIGNATURE",
                    "Custom deserializer method must be static with signature: public static Result<${0}> deserialize(DeserializationContext context)");

    // ==================== Package Errors ====================
    
    public static String unnamedPackageError(String typeName) {
        return String.format(
                "Configuration class '%s' must be in a named package. Add a package declaration (e.g., package com.example.config;)",
                typeName
        );
    }

    // ==================== Custom Deserializer Runtime Errors ====================
    
    public static String multipleDeserializersError(String typeName) {
        return String.format(
                "Multiple custom deserializers found for type '%s'. Only one deserializer per type is allowed.",
                typeName
        );
    }

    public static String missingCustomDeserializerAnnotation(String typeName) {
        return String.format(
                "Custom deserializer '%s' must be annotated with @CustomDeserializerFor(YourType.class)",
                typeName
        );
    }

    public static String invalidCustomDeserializerStructure(String typeName) {
        return String.format(
                "Custom deserializer '%s' must either implement CustomDeserializer interface or have a static deserialize method.",
                typeName
        );
    }
}
