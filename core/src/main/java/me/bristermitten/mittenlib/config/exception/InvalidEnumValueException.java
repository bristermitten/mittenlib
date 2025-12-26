package me.bristermitten.mittenlib.config.exception;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Exception thrown when an invalid value is provided for an enum property.
 * Provides detailed information including all valid enum values and suggestions.
 */
public class InvalidEnumValueException extends ConfigDeserialisationException {
    private final Class<? extends Enum<?>> enumClass;
    private final String propertyName;
    private final Object actualValue;

    public InvalidEnumValueException(Class<? extends Enum<?>> enumClass, String propertyName, Object actualValue) {
        this.enumClass = enumClass;
        this.propertyName = propertyName;
        this.actualValue = actualValue;
    }

    @Override
    public String getMessage() {
        String enumTypeName = enumClass.getSimpleName();
        Enum<?>[] constants = enumClass.getEnumConstants();
        
        // Get all valid values
        String validValues = Arrays.stream(constants)
                .map(Enum::name)
                .collect(Collectors.joining("', '", "'", "'"));
        
        // Find closest match for suggestion
        String suggestion = findClosestMatch(actualValue.toString(), constants);
        
        String suggestionText = suggestion != null 
                ? String.format("\n│  💡 Did you mean '%s'?", suggestion)
                : "";
        
        return String.format("""
                
                ╔════════════════════════════════════════════════════════════════════════════════╗
                ║                          INVALID ENUM VALUE                                    ║
                ╚════════════════════════════════════════════════════════════════════════════════╝
                
                An invalid value was provided for an enum property in your configuration.
                
                Property:      %s
                Expected Type: %s
                Provided:      '%s' (type: %s)
                
                ┌─ Valid values for %s:
                │
                │  %s%s
                │
                └─ Note: Enum values are case-sensitive unless configured otherwise.
                
                ════════════════════════════════════════════════════════════════════════════════
                """,
                propertyName,
                enumTypeName,
                actualValue,
                actualValue.getClass().getSimpleName(),
                enumTypeName,
                validValues,
                suggestionText
        );
    }
    
    /**
     * Finds the closest matching enum constant to the given input using Levenshtein distance.
     * Returns null if no close match is found.
     */
    private String findClosestMatch(String input, Enum<?>[] constants) {
        if (input == null || input.isEmpty()) {
            return null;
        }
        
        String bestMatch = null;
        int minDistance = Integer.MAX_VALUE;
        
        for (Enum<?> constant : constants) {
            String constantName = constant.name();
            int distance = levenshteinDistance(input.toLowerCase(), constantName.toLowerCase());
            
            // Consider it a close match if distance is <= 2 or <= 30% of the length
            if (distance < minDistance && (distance <= 2 || distance <= constantName.length() * 0.3)) {
                minDistance = distance;
                bestMatch = constantName;
            }
        }
        
        return bestMatch;
    }
    
    /**
     * Computes the Levenshtein distance between two strings.
     */
    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                    Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                );
            }
        }
        
        return dp[s1.length()][s2.length()];
    }
}
