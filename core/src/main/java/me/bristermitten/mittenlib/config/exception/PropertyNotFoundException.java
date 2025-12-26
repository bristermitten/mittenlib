package me.bristermitten.mittenlib.config.exception;

/**
 * Exception thrown when a required property is not found in a configuration file.
 * Provides detailed information to help users identify and fix the missing property.
 */
public class PropertyNotFoundException extends ConfigDeserialisationException {
    private final Class<?> configClass;
    private final String propertyName;
    private final String propertyType;
    private final String keyName;

    public PropertyNotFoundException(Class<?> configClass, String propertyName, String propertyType, String keyName) {
        this.configClass = configClass;
        this.propertyName = propertyName;
        this.propertyType = propertyType;
        this.keyName = keyName;
    }

    @Override
    public String getMessage() {
        String className = configClass.getSimpleName();
        
        return String.format("""
                
                ╔════════════════════════════════════════════════════════════════════════════════╗
                ║                          MISSING CONFIGURATION PROPERTY                        ║
                ╚════════════════════════════════════════════════════════════════════════════════╝
                
                A required property is missing from your configuration file.
                
                Configuration Class: %s
                Missing Property:    %s
                Expected Type:       %s
                Expected Key Name:   '%s'
                
                ┌─ What to do:
                │
                │  Add the missing property to your configuration file:
                │
                │  %s: <value>
                │
                │  Where <value> should be of type: %s
                │
                └─ Note: If this property is optional, add @Nullable annotation to the field
                   in your DTO class.
                
                ════════════════════════════════════════════════════════════════════════════════
                """,
                className,
                propertyName,
                propertyType,
                keyName,
                keyName,
                propertyType
        );
    }
}
