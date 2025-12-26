package me.bristermitten.mittenlib.annotations.parser;

import io.toolisticon.aptk.tools.corematcher.PlainValidationMessage;
import io.toolisticon.aptk.tools.corematcher.ValidationMessage;

/**
 * Compile-time validation error messages for configuration processing.
 * These messages help developers identify and fix issues in their configuration DTO classes.
 */
public class ConfigVerificationErrors {
    public static final ValidationMessage UNION_ALTERNATIVE_NOT_EXTENDING_UNION =
            PlainValidationMessage.create("UNION_ALTERNATIVE_NOT_EXTENDING_UNION",
                    """
                    ╔════════════════════════════════════════════════════════════════════════════════╗
                    ║                    UNION ALTERNATIVE INHERITANCE ERROR                         ║
                    ╚════════════════════════════════════════════════════════════════════════════════╝
                    
                    Alternative in union '${0}' MUST extend the union type when the union type 
                    has properties defined.
                    
                    ┌─ What to do:
                    │
                    │  Ensure that all alternatives in your union extend the base union type:
                    │
                    │  public class AlternativeClass extends ${0} {
                    │      // Alternative-specific fields
                    │  }
                    │
                    └─ Why: This ensures alternatives inherit all common properties defined in
                       the union base type, maintaining type consistency.
                    
                    ════════════════════════════════════════════════════════════════════════════════
                    """
            );

    public static final ValidationMessage ENUM_PARSING_SCHEME_NOT_ENUM =
            PlainValidationMessage.create("ENUM_PARSING_SCHEME_NOT_ENUM",
                    """
                    ╔════════════════════════════════════════════════════════════════════════════════╗
                    ║                    INVALID @EnumParsingScheme USAGE                            ║
                    ╚════════════════════════════════════════════════════════════════════════════════╝
                    
                    The @EnumParsingScheme annotation is applied to a property that is not an enum.
                    
                    ┌─ What to do:
                    │
                    │  1. Remove the @EnumParsingScheme annotation if this property should not be
                    │     an enum type.
                    │
                    │  2. Or change the property type to an enum if you intended to configure
                    │     enum parsing behavior.
                    │
                    │  Example:
                    │     @EnumParsingScheme(EnumParsingScheme.CASE_INSENSITIVE)
                    │     MyEnum propertyName;
                    │
                    └─ Note: @EnumParsingScheme only affects properties with enum types and
                       controls how string values are matched to enum constants.
                    
                    ════════════════════════════════════════════════════════════════════════════════
                    """
            );


    public static final ValidationMessage CUSTOM_DESERIALIZER_INVALID_STATIC_METHOD_SIGNATURE =
            PlainValidationMessage.create("CUSTOM_DESERIALIZER_INVALID_STATIC_METHOD_SIGNATURE",
                    """
                    ╔════════════════════════════════════════════════════════════════════════════════╗
                    ║                  INVALID CUSTOM DESERIALIZER SIGNATURE                         ║
                    ╚════════════════════════════════════════════════════════════════════════════════╝
                    
                    Custom deserializer method does not have the correct signature.
                    
                    ┌─ Required signature:
                    │
                    │  public static Result<${0}> deserialize(DeserializationContext context)
                    │
                    │  Where:
                    │    - Method must be static
                    │    - Return type must be Result<${0}>
                    │    - Must accept exactly one parameter: DeserializationContext
                    │
                    │  Example:
                    │
                    │    public static Result<${0}> deserialize(DeserializationContext context) {
                    │        // Your custom deserialization logic here
                    │        return Result.ok(yourValue);
                    │    }
                    │
                    └─ Tip: Use Result.ok(value) for successful deserialization or
                       Result.fail(exception) for errors.
                    
                    ════════════════════════════════════════════════════════════════════════════════
                    """
            );
}
