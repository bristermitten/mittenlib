package me.bristermitten.mittenlib.annotations.util;

public class StringUtil {
    private StringUtil() {
    }

    /**
     * Capitalize the first letter of a given String
     * <ul>
     * <li>"" => ""</li>
     * <li>"hello" => "Hello"</li>
     * <li>"HEllo" => "HEllo"</li>
     * </ul>
     *
     * @param str the String to capitalize
     * @return If the String is empty, then the string itself (i.e <code>s == capitalize(s)</code>),
     * otherwise a copy of the string with the first character capitalized
     */
    public static String capitalize(String str) {
        if (str.isEmpty()) {
            return str;
        }
        char first = str.charAt(0);
        return Character.toUpperCase(first) + str.substring(1);
    }
}
