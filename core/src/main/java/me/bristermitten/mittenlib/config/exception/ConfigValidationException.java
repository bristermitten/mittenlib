package me.bristermitten.mittenlib.config.exception;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.Objects;

public class ConfigValidationException extends IllegalArgumentException {
    private final Class<?> configClass;
    private final ImmutableCollection<Violation> violations;

    public ConfigValidationException(Class<?> configClass, Collection<Violation> violations) {
        super(formatMessage(configClass, violations));
        this.configClass = configClass;
        this.violations = ImmutableList.copyOf(violations);
    }

    public Class<?> getConfigClass() {
        return configClass;
    }

    public ImmutableCollection<Violation> getViolations() {
        return violations;
    }

    private static String formatMessage(Class<?> configClass, Collection<Violation> violations) {
        StringBuilder sb = new StringBuilder();
        sb.append("Configuration validation failed for class ")
                .append(configClass.getSimpleName())
                .append(" with ")
                .append(violations.size())
                .append(" violation(s):\n");
        for (Violation violation : violations) {
            sb.append(" - Property '")
                    .append(violation.propertyName())
                    .append("' (invalid value: ")
                    .append(violation.invalidValue())
                    .append("): ")
                    .append(violation.message())
                    .append("\n");
        }
        return sb.toString();
    }

    public static class Violation {
        private final String propertyName;
        private final Object invalidValue;
        private final String message;

        public Violation(String propertyName, Object invalidValue, String message) {
            this.propertyName = propertyName;
            this.invalidValue = invalidValue;
            this.message = message;
        }

        public String propertyName() {
            return propertyName;
        }

        public Object invalidValue() {
            return invalidValue;
        }

        public String message() {
            return message;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Violation)) return false;
            Violation violation = (Violation) o;
            return Objects.equals(propertyName, violation.propertyName)
                    && Objects.equals(invalidValue, violation.invalidValue)
                    && Objects.equals(message, violation.message);
        }

        @Override
        public int hashCode() {
            return Objects.hash(propertyName, invalidValue, message);
        }

        @Override
        public String toString() {
            return "Violation{"
                    + "propertyName='"
                    + propertyName
                    + '\''
                    + ", invalidValue="
                    + invalidValue
                    + ", message='"
                    + message
                    + '\''
                    + '}';
        }
    }
}
