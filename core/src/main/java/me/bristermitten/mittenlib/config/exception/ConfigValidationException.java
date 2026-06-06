package me.bristermitten.mittenlib.config.exception;

import java.util.Collection;
import java.util.Collections;

public class ConfigValidationException extends IllegalArgumentException {
    private final Class<?> configClass;
    private final Collection<Violation> violations;

    public ConfigValidationException(Class<?> configClass, Collection<Violation> violations) {
        super(formatMessage(configClass, violations));
        this.configClass = configClass;
        this.violations = Collections.unmodifiableCollection(violations);
    }

    public Class<?> getConfigClass() {
        return configClass;
    }

    public Collection<Violation> getViolations() {
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
            if (o == null || getClass() != o.getClass()) return false;
            Violation violation = (Violation) o;
            return java.util.Objects.equals(propertyName, violation.propertyName) &&
                   java.util.Objects.equals(invalidValue, violation.invalidValue) &&
                   java.util.Objects.equals(message, violation.message);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(propertyName, invalidValue, message);
        }

        @Override
        public String toString() {
            return "Violation{" +
                   "propertyName='" + propertyName + '\'' +
                   ", invalidValue=" + invalidValue +
                   ", message='" + message + '\'' +
                   '}';
        }
    }
}
