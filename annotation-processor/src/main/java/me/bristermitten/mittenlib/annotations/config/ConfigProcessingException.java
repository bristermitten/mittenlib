package me.bristermitten.mittenlib.annotations.config;

public class ConfigProcessingException extends RuntimeException {
    public ConfigProcessingException() {
    }

    public ConfigProcessingException(String message) {
        super(message);
    }

    public ConfigProcessingException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConfigProcessingException(Throwable cause) {
        super(cause);
    }

    public ConfigProcessingException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
