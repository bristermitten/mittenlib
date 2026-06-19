package me.bristermitten.mittenlib.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

/**
 * A {@link ParameterizedType} that can take a dynamic number of type arguments. Required when not
 * all types are known at compile time, otherwise {@link com.google.inject.TypeLiteral} can be used.
 * <a href="https://stackoverflow.com/a/49418496/6272977">Source / Credit</a>
 */
public class CompositeType implements ParameterizedType {
    private final Class<?> baseClass;
    private final Class<?>[] parameters;
    private final String name;

    /**
     * Creates a new CompositeType
     *
     * @param baseClass the base class
     * @param arguments the type arguments
     */
    public CompositeType(Class<?> baseClass, Class<?>... arguments) {
        this.baseClass = baseClass;
        this.parameters = arguments.clone();

        this.name = String.format(
                "%s<%s>",
                baseClass.getName(),
                Arrays.stream(arguments).map(Class::getName).collect(Collectors.joining(", ")));
    }

    @Override
    public Type[] getActualTypeArguments() {
        return parameters.clone();
    }

    @Override
    public Type getRawType() {
        return baseClass;
    }

    @Override
    @Nullable public Type getOwnerType() {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ParameterizedType)) return false;
        ParameterizedType that = (ParameterizedType) o;
        return Objects.equals(baseClass, that.getRawType()) && Arrays.equals(parameters, that.getActualTypeArguments());
    }

    @Override
    public int hashCode() {
        return Objects.hash(baseClass, Arrays.hashCode(parameters));
    }

    @Override
    public String getTypeName() {
        return name;
    }
}
