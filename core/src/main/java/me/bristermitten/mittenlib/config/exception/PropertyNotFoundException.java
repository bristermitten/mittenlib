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
    private final @Nullable String description;
    private final @Nullable String example;
    private final @Nullable String note;

    public PropertyNotFoundException(Class<?> configClass, String propertyName, String propertyType, String keyName) {
        this(configClass, propertyName, propertyType, keyName, null, null, null, null);
    }

    public PropertyNotFoundException(Class<?> configClass, String propertyName, String propertyType, String keyName, @Nullable Path configFilePath) {
        this(configClass, propertyName, propertyType, keyName, configFilePath, null, null, null);
    }
    
    public PropertyNotFoundException(Class<?> configClass, String propertyName, String propertyType, String keyName, 
                                    @Nullable Path configFilePath, @Nullable String description, 
                                    @Nullable String example, @Nullable String note) {
        this.configClass = configClass;
        this.propertyName = propertyName;
        this.propertyType = propertyType;
        this.keyName = keyName;
        this.configFilePath = configFilePath;
        this.description = description;
        this.example = example;
        this.note = note;
    }

    @Override
    public String getMessage() {
        String fileInfo = configFilePath != null 
                ? String.format("File: %s\n\n", configFilePath)
                : "";
        
        String descriptionInfo = description != null && !description.isEmpty()
                ? String.format("%s\n\n", description)
                : "";
        
        String exampleValue = example != null && !example.isEmpty()
                ? example
                : "<value>";
        
        String noteInfo = note != null && !note.isEmpty()
                ? String.format("\nNote: %s\n", note)
                : "";
        
        return String.format("""
                
                ╔════════════════════════════════════════════════════════════════════════════════╗
                ║                         CONFIG ERROR: Missing Setting                          ║
                ╚════════════════════════════════════════════════════════════════════════════════╝
                
                %s%sMissing required setting: %s
                
                Add this to your config:
                    %s: <value>
                
                Example: %s: %s%s
                ════════════════════════════════════════════════════════════════════════════════
                """,
                fileInfo,
                descriptionInfo,
                keyName,
                keyName,
                keyName,
                exampleValue,
                noteInfo
        );
    }
}
