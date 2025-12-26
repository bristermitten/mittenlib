package me.bristermitten.mittenlib.annotations.codegen;

/**
 * Constants for commonly used names in code generation.
 * This centralizes hardcoded strings to make them easier to maintain and modify.
 */
public final class CodeGenNames {
    private CodeGenNames() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Common parameter and variable names used throughout code generation.
     */
    public static final class Variables {
        public static final String CONTEXT = "context";
        public static final String DAO = "dao";
        public static final String DATA = "$data";
        public static final String PARENT = "parent";
        public static final String ENUM_VALUE = "enumValue";
        public static final String MAP_DATA = "mapData";
        
        private Variables() {
            throw new UnsupportedOperationException("Utility class");
        }
    }

    /**
     * Suffix patterns for generated variable names.
     */
    public static final class Suffixes {
        public static final String FROM_MAP = "FromMap";
        
        private Suffixes() {
            throw new UnsupportedOperationException("Utility class");
        }
    }

    /**
     * Method name prefixes and patterns.
     */
    public static final class Methods {
        public static final String DESERIALIZE_PREFIX = "deserialize";
        public static final String SERIALIZE_PREFIX = "serialize";
        
        private Methods() {
            throw new UnsupportedOperationException("Utility class");
        }
    }
}
