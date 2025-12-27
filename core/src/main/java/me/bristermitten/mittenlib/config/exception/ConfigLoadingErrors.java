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
                
                %sYour configuration has the wrong type of value for a setting.
                
                Setting: %s
                Your value: %s
                The problem: This needs to be %s, but you provided %s
                
                ┌─ How to fix:
                │
                │  1. Open your config file
                │  2. Find the line: %s: %s
                │  3. Change the value to be %s
                │
                ├─ Examples of %s values:
                │  %s
                │
                └─ Common mistakes:
                   - Numbers should NOT have quotes: use 123 not "123"
                   - Text SHOULD have quotes: use "hello" not hello
                   - True/false should NOT have quotes: use true not "true"
                
                ════════════════════════════════════════════════════════════════════════════════
                """,
                fileInfo,
                propertyName,
                actualValue,
                friendlyExpectedType,
                friendlyActualType,
                propertyName,
                actualValue,
                friendlyExpectedType,
                friendlyExpectedType,
                getExampleValues(expectedType)
        ));
    }
    
    private static String getFriendlyTypeName(String typeName) {
        return switch (typeName.toLowerCase()) {
            case "int", "integer", "long", "short", "byte" -> "a number";
            case "double", "float" -> "a decimal number";
            case "string" -> "text";
            case "boolean" -> "true or false";
            default -> "a " + typeName;
        };
    }
    
    private static String getExampleValues(String typeName) {
        return switch (typeName.toLowerCase()) {
            case "int", "integer", "long", "short", "byte" -> "  - 1, 10, 100, 9999";
            case "double", "float" -> "  - 1.5, 10.0, 99.99";
            case "string" -> "  - \"hello\", \"world\", \"localhost\"";
            case "boolean" -> "  - true, false";
            default -> "  - (check plugin documentation)";
        };
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
                
                %sYour configuration file has an invalid format or structure.
                
                ┌─ How to fix:
                │
                │  1. Check the plugin's documentation or example config
                │  2. Make sure your config structure matches the examples
                │  3. Verify all required settings are present
                │  4. Check for typos in setting names
                │  5. Make sure indentation is correct (YAML files are indent-sensitive)
                │
                └─ Tip: You can usually find an example config file on the plugin's page
                   or in the plugin's folder. Try comparing your config to the example.
                
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
