package me.bristermitten.mittenlib.watcher;

import org.jetbrains.annotations.NotNull;

/**
 * Exception thrown when there is an error with file watching operations.
 */
public class FileWatcherException extends RuntimeException {
    /**
     * Creates a new FileWatcherException with the specified message and cause.
     *
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public FileWatcherException(@NotNull String message, @NotNull Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new FileWatcherException with the specified message.
     *
     * @param message the detail message
     */
    public FileWatcherException(@NotNull String message) {
        super(message);
    }
}