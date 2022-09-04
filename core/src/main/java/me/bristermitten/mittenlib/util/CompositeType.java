package me.bristermitten.mittenlib.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.stream.Collectors;


/**
 * A {@link ParameterizedType} that can take a dynamic number of type arguments.
 * Required when not all types are known at compile time, otherwise {@link com.google.inject.TypeLiteral} can be used.
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
        this.parameters = arguments;

        this.name = String.format("%s<%s>", baseClass.getName(),
                Arrays.stream(arguments)
                        .map(Class::getName)
                        .collect(Collectors.joining(", ")));
    }


    @Override
    public Type[] getActualTypeArguments() {
        return parameters;
    }

    @Override
    public Type getRawType() {
        return baseClass;
    }

    @Override
    public Type getOwnerType() {
        return null;
    }

    @Override
    public String getTypeName() {
        return name;
    }
}
