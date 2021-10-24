package me.bristermitten.mittenlib.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.stream.Collectors;

// Credit: https://stackoverflow.com/a/49418496/6272977
public class CompositeType implements ParameterizedType {
    private final Class<?> baseClass;
    private final Class<?>[] parameters;
    private final String name;

    public CompositeType(Class<?> baseClass, Class<?>... parameters) {
        this.baseClass = baseClass;
        this.parameters = parameters;

        this.name = String.format("%s<%s>", baseClass.getName(),
                Arrays.stream(parameters)
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
