package me.bristermitten.mittenlib.util;

public class Errors {
    private Errors() {

    }
    public static <E extends Throwable> void sneakyThrow(Throwable e) throws E {
        //noinspection unchecked
        throw (E) e;
    }

}
