package me.bristermitten.mittenlib.config.exception;

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
        return "\n====================================================\n" +
               "Property not found in config: " + configClass.getName() + "\n" +
               "Property name: " + propertyName + "\n" +
               "Property type: " + propertyType + "\n\n" +
               "We expected to see a key named " + keyName + ", but there was nothing present under this key\n" +
               "====================================================\n";
    }
}
