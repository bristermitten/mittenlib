package me.bristermitten.mittenlib.annotations.exception;

/**
 * General exception thrown when the config annotation processor fails
 */
public class ConfigProcessingException extends RuntimeException {

    /**
     * Create a new ConfigProcessingException with the given message and cause
     *
     * @param message the message
     * @param cause   the cause
     * @see RuntimeException#RuntimeException(String, Throwable)
     */
    public ConfigProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
