package me.bristermitten.mittenlib.annotations.exception

import java.lang.RuntimeException

/**
 * General exception thrown when the config annotation processor fails
 */
class ConfigProcessingException
/**
 * Create a new ConfigProcessingException with the given message and cause
 *
 * @param message the message
 * @param cause   the cause
 * @see RuntimeException.RuntimeException
 */
    (message: String?, cause: Throwable?) : RuntimeException(message, cause)
