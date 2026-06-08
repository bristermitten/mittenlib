package me.bristermitten.mittenlib.annotations.parser;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.squareup.javapoet.TypeName;

import javax.lang.model.type.TypeMirror;
import java.util.Optional;

public abstract class CustomInfoRegistry<T> {
    private final Multimap<TypeName, T> infoMultimap = HashMultimap.create();

    public void register(TypeName clazz, T info) {
        infoMultimap.put(clazz, info);
    }


    public Optional<T> getCustomInfo(TypeMirror propertyType) {
        var fromMap = infoMultimap.get(TypeName.get(propertyType));

        if (fromMap.isEmpty()) {
            return Optional.empty();
        }
        if (fromMap.size() > 1) {
            throw new IllegalArgumentException("Not sure how to handle multiple yet");
        }

        return Optional.of(fromMap.iterator().next());
    }

}
