package me.bristermitten.mittenlib.annotations.util;

public class StringUtil {
    private StringUtil() {
    }

    public static String capitalize(String str) {
        if (str.isEmpty()) {
            return str;
        }
        char first = str.charAt(0);
        return Character.toUpperCase(first) + str.substring(1);
    }
}
