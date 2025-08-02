package me.bristermitten.mittenlib.config.tree;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Like {@link com.google.gson.JsonElement} but without a strict dependency on json
 */
public abstract class DataTree {

    private DataTree() {

    }

    public abstract Object value();

    public @Nullable DataTree get(String key) {
        return null; // not an object
    }

    @Contract("_, !null -> !null; _ , _-> _")
    public Object getOrDefault(@NotNull String key, @Nullable Object defaultValue) {
        DataTree get = this.get(key);
        if (get == null) {
            return defaultValue;
        }
        return get;
    }

    public static class DataTreeNull extends DataTree {
        public static final DataTreeNull INSTANCE = new DataTreeNull();

        private DataTreeNull() {
        }

        @Override
        public Object value() {
            return null;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof DataTreeNull;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(this);
        }

        @Override
        public String toString() {
            return "DataTreeNull{}";
        }
    }

    public static abstract class DataTreeLiteral extends DataTree {

        @Override
        public abstract Object value();

        public static class DataTreeLiteralInt extends DataTreeLiteral {
            public final long value;

            public DataTreeLiteralInt(long value) {
                this.value = value;
            }

            @Override
            public boolean equals(Object o) {
                if (!(o instanceof DataTreeLiteralInt)) return false;
                DataTreeLiteralInt that = (DataTreeLiteralInt) o;
                return value == that.value;
            }

            @Override
            public int hashCode() {
                return Long.hashCode(value);
            }

            @Override
            public Long value() {
                return value;
            }
        }

        public static class DataTreeLiteralFloat extends DataTreeLiteral {
            public final double value;

            public DataTreeLiteralFloat(double value) {
                this.value = value;
            }

            @Override
            public boolean equals(Object o) {
                if (!(o instanceof DataTreeLiteralFloat)) return false;
                DataTreeLiteralFloat that = (DataTreeLiteralFloat) o;
                return Double.compare(value, that.value) == 0;
            }

            @Override
            public int hashCode() {
                return Double.hashCode(value);
            }

            @Override
            public Double value() {
                return value;
            }
        }

        public static class DataTreeLiteralString extends DataTreeLiteral {
            public final String value;

            public DataTreeLiteralString(String value) {
                this.value = value;
            }

            @Override
            public boolean equals(Object o) {
                if (!(o instanceof DataTreeLiteralString)) return false;
                DataTreeLiteralString that = (DataTreeLiteralString) o;
                return Objects.equals(value, that.value);
            }

            @Override
            public int hashCode() {
                return Objects.hashCode(value);
            }

            @Override
            public String value() {
                return value;
            }
        }

        public static class DataTreeLiteralBoolean extends DataTreeLiteral {
            public final boolean value;

            public DataTreeLiteralBoolean(boolean value) {
                this.value = value;
            }

            @Override
            public boolean equals(Object o) {
                if (!(o instanceof DataTreeLiteralBoolean)) return false;
                DataTreeLiteralBoolean that = (DataTreeLiteralBoolean) o;
                return value == that.value;
            }

            @Override
            public int hashCode() {
                return Boolean.hashCode(value);
            }

            @Override
            public Boolean value() {
                return value;
            }
        }
    }

    public static class DataTreeArray extends DataTree {
        public final DataTree[] values;

        public DataTreeArray(DataTree[] values) {
            this.values = values;
        }

        public DataTree[] getValues() {
            return values;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof DataTreeArray)) return false;
            DataTreeArray that = (DataTreeArray) o;
            return Objects.deepEquals(values, that.values);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(values);
        }

        @Override
        public List<DataTree> value() {
            return Arrays.asList(values);
        }
    }

    public static class DataTreeMap extends DataTree {
        private final Map<DataTree, DataTree> values;

        public DataTreeMap(Map<DataTree, DataTree> values) {
            this.values = values;
        }

        public Map<DataTree, DataTree> values() {
            return values;
        }

        @Override
        public Map<DataTree, DataTree> value() {
            return values;
        }

        @Override
        public @Nullable DataTree get(String key) {
            return values.get(new DataTreeLiteral.DataTreeLiteralString(key));
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof DataTreeMap)) return false;
            DataTreeMap that = (DataTreeMap) o;
            return Objects.equals(values, that.values);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(values);
        }
    }
}
