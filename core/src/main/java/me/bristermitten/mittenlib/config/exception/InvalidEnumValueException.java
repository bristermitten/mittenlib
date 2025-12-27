package me.bristermitten.mittenlib.config.exception;

import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Exception thrown when an invalid value is provided for an enum property.
 * Provides detailed information including all valid enum values and suggestions.
 */
public class InvalidEnumValueException extends ConfigDeserialisationException {
    /** Maximum Levenshtein distance for a close match suggestion */
    private static final int MAX_DISTANCE_THRESHOLD = 2;
    
    /** Maximum distance as a percentage of string length for a close match */
    private static final double MAX_DISTANCE_PERCENTAGE = 0.3;
    
    private final Class<? extends Enum<?>> enumClass;
    private final String propertyName;
    private final Object actualValue;
    private final @Nullable Path configFilePath;

    public InvalidEnumValueException(Class<? extends Enum<?>> enumClass, String propertyName, Object actualValue) {
        this(enumClass, propertyName, actualValue, null);
    }

    public InvalidEnumValueException(Class<? extends Enum<?>> enumClass, String propertyName, Object actualValue, @Nullable Path configFilePath) {
        this.enumClass = enumClass;
        this.propertyName = propertyName;
        this.actualValue = actualValue;
        this.configFilePath = configFilePath;
    }

    @Override
    public String getMessage() {
        Enum<?>[] constants = enumClass.getEnumConstants();
        
        // Get all valid values
        String validValues = Arrays.stream(constants)
                .map(Enum::name)
                .collect(Collectors.joining("', '", "'", "'"));
        
        // Find closest match for suggestion
        String suggestion = findClosestMatch(actualValue.toString(), constants);
        
        String suggestionText = suggestion != null 
                ? String.format("\n│  Did you mean '%s'?", suggestion)
                : "";
        
        String fileInfo = configFilePath != null 
                ? String.format("File: %s\n\n", configFilePath)
                : "";
        
        return String.format("""
                
                ╔════════════════════════════════════════════════════════════════════════════════╗
                ║                         CONFIG ERROR: Invalid Value                            ║
                ╚════════════════════════════════════════════════════════════════════════════════╝
                
                %sYour configuration has an invalid value.
                
                Setting: %s
                Your value: '%s'
                
                ┌─ Valid options:
                │
                │  %s%s
                │
                ├─ How to fix:
                │
                │  1. Open your config file
                │  2. Find the line: %s: %s
                │  3. Change '%s' to one of the valid options above
                │  4. Make sure you match the spelling exactly (including uppercase/lowercase)
                │  5. Save the file and restart your server
                │
                └─ Note: The options are case-sensitive, so 'OPTION' is different from 'option'
                
                ════════════════════════════════════════════════════════════════════════════════
                """,
                fileInfo,
                propertyName,
                actualValue,
                validValues,
                suggestionText,
                propertyName,
                actualValue,
                actualValue
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
        
        String inputLower = input.toLowerCase();
        String bestMatch = null;
        int minDistance = Integer.MAX_VALUE;
        
        for (Enum<?> constant : constants) {
            String constantName = constant.name();
            String constantLower = constantName.toLowerCase();
            int distance = levenshteinDistance(inputLower, constantLower);
            
            // Consider it a close match if distance is <= threshold or <= percentage of the length
            if (distance < minDistance && 
                (distance <= MAX_DISTANCE_THRESHOLD || 
                 distance <= constantName.length() * MAX_DISTANCE_PERCENTAGE)) {
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
