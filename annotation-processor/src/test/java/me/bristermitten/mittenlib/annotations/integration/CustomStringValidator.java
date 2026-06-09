package me.bristermitten.mittenlib.annotations.integration;

import com.google.inject.Inject;
import me.bristermitten.mittenlib.config.validation.Validator;

import java.util.Optional;

public class CustomStringValidator implements Validator<String> {
    private final ValidationDependency dependency;

    @Inject
    public CustomStringValidator(ValidationDependency dependency) {
        this.dependency = dependency;
    }

    @Override
    public Optional<String> validate(String value) {
        if (dependency.check(value)) {
            return Optional.empty();
        }
        return Optional.of("Must start with expected prefix, but was '" + value + "'");
    }

    /** A dummy dependency that the Validator needs. */
    public static class ValidationDependency {
        private final String prefix;

        // used in other tests where the manually constructed instance isn't explicitly bound
        public ValidationDependency() {
            this("mitten-lib");
        }

        public ValidationDependency(String prefix) {
            this.prefix = prefix;
        }

        public boolean check(String val) {
            return val != null && val.startsWith(prefix);
        }
    }
}
