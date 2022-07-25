package me.bristermitten.mittenlib.annotations.config;

/**
 * General exception thrown when the config annotation processor fails
 */
public class ConfigProcessingException extends RuntimeException {
    /**
     * @see RuntimeException#RuntimeException(String)
     */
    public ConfigProcessingException() {
        super();
    }

    /**
     * @see RuntimeException#RuntimeException(String)
     */
    public ConfigProcessingException(String message) {
        super(message);
    }

    /**
     * @see RuntimeException#RuntimeException(String, Throwable)
     */
    public ConfigProcessingException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @see RuntimeException#RuntimeException(Throwable)
     */
    public ConfigProcessingException(Throwable cause) {
        super(cause);
    }

    /**
     * @see RuntimeException#RuntimeException(String, Throwable, boolean, boolean)
     */
    public ConfigProcessingException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
