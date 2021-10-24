package me.bristermitten.mittenlib.util;

public interface SafeRunnable {
    void run() throws Throwable;

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
