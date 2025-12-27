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
    
    /**
     * Creates an exception to use when a value cannot be deserialized as it is not found (i.e. is null)
     * This version includes file path information and custom documentation from PropertyDoc annotation
     *
     * @param fieldName      the name of the field that is trying to be deserialized
     * @param typeName       the name of the type that is trying to be deserialized
     * @param enclosingClass the name of the enclosing class
     * @param keyName        the key name in the config file
     * @param context        the deserialization context containing file path information
     * @param description    custom description from @PropertyDoc
     * @param example        custom example from @PropertyDoc
     * @param note           custom note from @PropertyDoc
     * @return the exception to throw
     */
    public static RuntimeException notFoundException(String fieldName, String typeName, Class<?> enclosingClass, String keyName, 
                                                     @Nullable DeserializationContext context, @Nullable String description, 
                                                     @Nullable String example, @Nullable String note) {
        Path sourcePath = context != null ? context.getSourcePath() : null;
        return new PropertyNotFoundException(enclosingClass, fieldName, typeName, keyName, sourcePath, description, example, note);
    }


    public static RuntimeException invalidPropertyTypeException(Class<?> enclosingClass, String propertyName, String expectedType, Object actualValue) {
        return invalidPropertyTypeException(enclosingClass, propertyName, expectedType, actualValue, null);
    }

    public static RuntimeException invalidPropertyTypeException(Class<?> enclosingClass, String propertyName, String expectedType, Object actualValue, @Nullable Path configFilePath) {
        String fileInfo = configFilePath != null 
                ? String.format("File: %s\n\n", configFilePath)
                : "";
        
        // Simplify type names for common types
        String friendlyExpectedType = getFriendlyTypeName(expectedType);
        String friendlyActualType = getFriendlyTypeName(actualValue.getClass().getSimpleName());
        
        return new IllegalArgumentException(String.format("""
                
                ╔════════════════════════════════════════════════════════════════════════════════╗
                ║                         CONFIG ERROR: Wrong Type                               ║
                ╚════════════════════════════════════════════════════════════════════════════════╝
                
                %sSetting '%s' has wrong type
                
                Expected: %s
                Got: %s (value: %s)
                
                Examples of %s: %s
                
                Tip: Numbers shouldn't have quotes, text should have quotes
                
                ════════════════════════════════════════════════════════════════════════════════
                """,
                fileInfo,
                propertyName,
                friendlyExpectedType,
                friendlyActualType,
                actualValue,
                friendlyExpectedType,
                getExampleValues(expectedType)
        ));
    }
    
    private static String getFriendlyTypeName(String typeName) {
        String lowerType = typeName.toLowerCase();
        if (lowerType.equals("int") || lowerType.equals("integer") || 
            lowerType.equals("long") || lowerType.equals("short") || lowerType.equals("byte")) {
            return "a number";
        } else if (lowerType.equals("double") || lowerType.equals("float")) {
            return "a decimal number";
        } else if (lowerType.equals("string")) {
            return "text";
        } else if (lowerType.equals("boolean")) {
            return "true or false";
        } else {
            return "a " + typeName;
        }
    }
    
    private static String getExampleValues(String typeName) {
        String lowerType = typeName.toLowerCase();
        if (lowerType.equals("int") || lowerType.equals("integer") || 
            lowerType.equals("long") || lowerType.equals("short") || lowerType.equals("byte")) {
            return "  - 1, 10, 100, 9999";
        } else if (lowerType.equals("double") || lowerType.equals("float")) {
            return "  - 1.5, 10.0, 99.99";
        } else if (lowerType.equals("string")) {
            return "  - \"hello\", \"world\", \"localhost\"";
        } else if (lowerType.equals("boolean")) {
            return "  - true, false";
        } else {
            return "  - (check plugin documentation)";
        }
    }

    public static RuntimeException noUnionMatch() {
        return noUnionMatch(null);
    }

    public static RuntimeException noUnionMatch(@Nullable Path configFilePath) {
        String fileInfo = configFilePath != null 
                ? String.format("File: %s\n\n", configFilePath)
                : "";
        
        return new IllegalArgumentException(String.format("""
                
                ╔════════════════════════════════════════════════════════════════════════════════╗
                ║                         CONFIG ERROR: Invalid Format                           ║
                ╚════════════════════════════════════════════════════════════════════════════════╝
                
                %sConfiguration structure doesn't match any expected format
                
                Check your config against the plugin's example or documentation
                
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
