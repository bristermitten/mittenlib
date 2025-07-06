package me.bristermitten.mittenlib.config.exception;

public class ConfigLoadingErrors {

    /**
     * Creates an exception to use when a value cannot be deserialized as it is not found (i.e. is null)
     *
     * @param fieldName      the name of the field that is trying to be deserialized
     * @param typeName       the name of the type that is trying to be deserialized
     * @param enclosingClass the name of the enclosing class
     * @return the exception to throw
     */
    public static RuntimeException notFoundException(String fieldName, String typeName, Class<?> enclosingClass, String keyName) {
        return new PropertyNotFoundException(enclosingClass, fieldName, typeName, keyName);
    }


    public static RuntimeException invalidPropertyTypeException(Class<?> enclosingClass, String propertyName, String expectedType, Object actualValue) {
        return new IllegalArgumentException(
                "Invalid value for property " + enclosingClass.getSimpleName() + "." + propertyName + ", expected value of type " + expectedType + " but got value " + actualValue + " of type " + actualValue.getClass()
        );
    }

    public static RuntimeException noUnionMatch() {
        return new IllegalArgumentException("No union alternatives matched.");
    }
}
