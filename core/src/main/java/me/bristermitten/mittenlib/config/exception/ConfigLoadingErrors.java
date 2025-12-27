package me.bristermitten.mittenlib.config.exception;

import me.bristermitten.mittenlib.config.DeserializationContext;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;

/**
 * Factory class for creating configuration loading error exceptions.
 * Provides user-friendly error messages for common configuration issues.
 */
public class ConfigLoadingErrors {

    /**
     * Creates an exception to use when a value cannot be deserialized as it is not found (i.e. is null)
     *
     * @param fieldName      the name of the field that is trying to be deserialized
     * @param typeName       the name of the type that is trying to be deserialized
     * @param enclosingClass the name of the enclosing class
     * @param keyName        the key name in the config file
     * @return the exception to throw
     */
    public static RuntimeException notFoundException(String fieldName, String typeName, Class<?> enclosingClass, String keyName) {
        return new PropertyNotFoundException(enclosingClass, fieldName, typeName, keyName);
    }

    /**
     * Creates an exception to use when a value cannot be deserialized as it is not found (i.e. is null)
     * This version includes file path information from the deserialization context
     *
     * @param fieldName      the name of the field that is trying to be deserialized
     * @param typeName       the name of the type that is trying to be deserialized
     * @param enclosingClass the name of the enclosing class
     * @param keyName        the key name in the config file
     * @param context        the deserialization context containing file path information
     * @return the exception to throw
     */
    public static RuntimeException notFoundException(String fieldName, String typeName, Class<?> enclosingClass, String keyName, @Nullable DeserializationContext context) {
        Path sourcePath = context != null ? context.getSourcePath() : null;
        return new PropertyNotFoundException(enclosingClass, fieldName, typeName, keyName, sourcePath);
    }


    public static RuntimeException invalidPropertyTypeException(Class<?> enclosingClass, String propertyName, String expectedType, Object actualValue) {
        return invalidPropertyTypeException(enclosingClass, propertyName, expectedType, actualValue, null);
    }

    public static RuntimeException invalidPropertyTypeException(Class<?> enclosingClass, String propertyName, String expectedType, Object actualValue, @Nullable Path configFilePath) {
        String actualTypeName = actualValue.getClass().getSimpleName();
        String className = enclosingClass.getSimpleName();
        String fileInfo = configFilePath != null 
                ? String.format("Configuration File:  %s\n", configFilePath)
                : "";
        
        return new IllegalArgumentException(String.format("""
                
                ╔════════════════════════════════════════════════════════════════════════════════╗
                ║                          INVALID PROPERTY TYPE                                 ║
                ╚════════════════════════════════════════════════════════════════════════════════╝
                
                A property has an incorrect type in your configuration file.
                
                %sConfiguration Class: %s
                Property:            %s
                Expected Type:       %s
                Actual Value:        %s
                Actual Type:         %s
                
                ┌─ What to do:
                │
                │  Update the value in your configuration file to match the expected type.
                │
                │  For %s, provide a value of type: %s
                │
                └─ Tip: Check that quotes, numbers, and boolean values are formatted correctly.
                
                ════════════════════════════════════════════════════════════════════════════════
                """,
                fileInfo,
                className,
                propertyName,
                expectedType,
                actualValue,
                actualTypeName,
                propertyName,
                expectedType
        ));
    }

    public static RuntimeException noUnionMatch() {
        return noUnionMatch(null);
    }

    public static RuntimeException noUnionMatch(@Nullable Path configFilePath) {
        String fileInfo = configFilePath != null 
                ? String.format("Configuration File:  %s\n\n", configFilePath)
                : "";
        
        return new IllegalArgumentException(String.format("""
                
                ╔════════════════════════════════════════════════════════════════════════════════╗
                ║                          NO UNION ALTERNATIVE MATCHED                          ║
                ╚════════════════════════════════════════════════════════════════════════════════╝
                
                None of the union alternatives matched the provided configuration data.
                
                %s┌─ What to do:
                │
                │  A union type requires that the data matches at least one of its alternatives.
                │  Check your configuration structure and ensure it matches one of the expected
                │  formats for this union type.
                │
                └─ Tip: Review the documentation for this configuration to see what alternative
                   formats are supported.
                
                ════════════════════════════════════════════════════════════════════════════════
                """,
                fileInfo
        ));
    }

    public static RuntimeException invalidEnumException(Class<? extends Enum<?>> enumClass, String propertyName, Object actualValue) {
        return new InvalidEnumValueException(enumClass, propertyName, actualValue);
    }

    public static RuntimeException invalidEnumException(Class<? extends Enum<?>> enumClass, String propertyName, Object actualValue, @Nullable Path configFilePath) {
        return new InvalidEnumValueException(enumClass, propertyName, actualValue, configFilePath);
    }

    public static RuntimeException defaultValueProxyException(Class<?> configClass, String propertyName) {
        return new UnsupportedOperationException(String.format("""
                
                ╔════════════════════════════════════════════════════════════════════════════════╗
                ║                          DEFAULT VALUE PROXY ERROR                             ║
                ╚════════════════════════════════════════════════════════════════════════════════╝
                
                An internal error occurred: default value proxy was invoked unexpectedly.
                
                Configuration Class: %s
                Property:            %s
                
                This is likely a bug in the configuration processor. Please report this issue
                with details about your configuration structure.
                
                ════════════════════════════════════════════════════════════════════════════════
                """,
                configClass.getSimpleName(),
                propertyName
        ));
    }
}
