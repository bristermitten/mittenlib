package me.bristermitten.mittenlib.annotations.exception

class ConfigProcessingException(message: String, cause: Throwable)
    extends RuntimeException(message, cause)
