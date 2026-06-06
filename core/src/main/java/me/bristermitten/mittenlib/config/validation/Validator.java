package me.bristermitten.mittenlib.config.validation;

import java.util.Optional;

public interface Validator<T> {
    /**
     * Validates the given value.
     *
     * @param value the value to validate (can be null if the field is nullable)
     * @return an empty Optional if the value is valid, or an Optional containing the error message if invalid
     */
    Optional<String> validate(T value);
}
