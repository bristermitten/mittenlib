package me.bristermitten.mittenlib.annotations.config;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import javax.inject.Singleton;
import javax.lang.model.element.TypeElement;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
public class GeneratedTypeCache {
    /**
     * Map of source type to generated type's qualified name
     */
    private final BiMap<TypeElement, String> generatedSpecs = HashBiMap.create();


    public BiMap<TypeElement, String> getGeneratedSpecs() {
        return generatedSpecs;
    }


    public Set<TypeElement> getByName(String name) {
        return generatedSpecs.entrySet()
                .stream()
                .filter(entry -> entry.getValue().contains(name) || name.contains(entry.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }
}
