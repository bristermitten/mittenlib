package me.bristermitten.mittenlib.config.exception;

import org.jspecify.annotations.Nullable;

import java.nio.file.Path;

/**
 * Exception thrown when a required property is not found in a configuration file.
 * Provides detailed information to help users identify and fix the missing property.
 */
public class PropertyNotFoundException extends ConfigDeserialisationException {
    private final Class<?> configClass;
    private final String propertyName;
    private final String propertyType;
    private final String keyName;
    private final @Nullable Path configFilePath;

    public PropertyNotFoundException(Class<?> configClass, String propertyName, String propertyType, String keyName) {
        this(configClass, propertyName, propertyType, keyName, null);
    }

    public PropertyNotFoundException(Class<?> configClass, String propertyName, String propertyType, String keyName, @Nullable Path configFilePath) {
        this.configClass = configClass;
        this.propertyName = propertyName;
        this.propertyType = propertyType;
        this.keyName = keyName;
        this.configFilePath = configFilePath;
    }

    @Override
    public String getMessage() {
        String fileInfo = configFilePath != null 
                ? String.format("File: %s\n\n", configFilePath)
                : "";
        
        return String.format("""
                
                ╔════════════════════════════════════════════════════════════════════════════════╗
                ║                         CONFIG ERROR: Missing Setting                          ║
                ╚════════════════════════════════════════════════════════════════════════════════╝
                
                %sMissing required setting: %s
                
                Add this to your config:
                    %s: <value>
                
                Example: %s: localhost
                
                ════════════════════════════════════════════════════════════════════════════════
                """,
                fileInfo,
                keyName,
                keyName,
                keyName
        );
    }
}
