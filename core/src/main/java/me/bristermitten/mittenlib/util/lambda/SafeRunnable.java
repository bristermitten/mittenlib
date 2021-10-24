package me.bristermitten.mittenlib.util.lambda;

import me.bristermitten.mittenlib.util.Errors;
import me.bristermitten.mittenlib.util.Result;
import me.bristermitten.mittenlib.util.Unit;

@FunctionalInterface
public interface SafeRunnable {
    void run() throws Throwable;

    default Result<Unit> runCatching() {
        return Result.runCatching(() -> {
            run();
            return Unit.UNIT;
        });
    }

    default Runnable asRunnable() {
        return () -> {
            try {
                run();
            } catch (Throwable e) {
                Errors.sneakyThrow(e);
            }
        };
    }
}
