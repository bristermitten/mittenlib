package me.bristermitten.mittenlib.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Exception representing multiple failures that occur simultaneously.
 * This is commonly used to collect and report multiple exceptions or errors
 * encountered during a single operation or execution context.
 * <p>
 * This exception is useful for aggregating multiple causes, allowing them
 * to be handled collectively rather than individually. It enables the capture
 * of all contributing failures for comprehensive error reporting.
 */
public class MultipleFailuresException extends RuntimeException {
    private final List<? extends Throwable> failures;

    public MultipleFailuresException(String message, List<Throwable> failures) {
        super(message);
        this.failures = new ArrayList<>(failures);
    }

    public MultipleFailuresException(List<Throwable> failures) {
        this("Multiple failures occurred", failures);
    }

    public List<Throwable> getFailures() {
        return Collections.unmodifiableList(failures);
    }

    @Override
    public String getMessage() {
        StringBuilder message = new StringBuilder(super.getMessage());
        message.append(":\n");
        for (int i = 0; i < failures.size(); i++) {
            message.append(String.format("%d: %s\n", i + 1, failures.get(i).getMessage()));
        }
        return message.toString();
    }

    @SuppressWarnings("CallToPrintStackTrace")
    @Override
    public void printStackTrace() {
        super.printStackTrace();

        for (Throwable failure : failures) {
            System.err.println("Caused by: " + failure);
            failure.printStackTrace();
        }
    }
}
