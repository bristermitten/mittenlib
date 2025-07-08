package me.bristermitten.mittenlib.config.exception;

public class InvalidEnumValueException extends ConfigDeserialisationException {
    private final Class<? extends Enum<?>> enumClass;
    private final String propertyName;
    private final Object actualValue;

    public InvalidEnumValueException(Class<? extends Enum<?>> enumClass, String propertyName, Object actualValue) {
        this.enumClass = enumClass;
        this.propertyName = propertyName;
        this.actualValue = actualValue;
    }

    @Override
    public String getMessage() {
        return "Invalid enum value for property " + propertyName + ", expected value of type " + enumClass.getSimpleName() + " but got value " + actualValue + " of type " + actualValue.getClass();
    }


}
