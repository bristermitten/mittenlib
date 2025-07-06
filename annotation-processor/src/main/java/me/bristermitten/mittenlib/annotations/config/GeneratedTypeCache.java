package me.bristermitten.mittenlib.annotations.config;

import com.google.common.annotations.Beta;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import me.bristermitten.mittenlib.config.GeneratedConfig;
import org.jetbrains.annotations.NotNull;

import javax.inject.Singleton;
import javax.lang.model.element.TypeElement;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <b>This class should not be treated as a public API, and is subject to change at any time.</b>
 * <p>
 * A cache of generated types.
 * This class is a pretty big hack to be honest, but it allows us to generate pretty error messages
 * when people accidentally use the generated type names in a DTO class.
 * <p>
 * This class maintains a {@link com.google.common.collect.BiMap} between the DTO {@link javax.lang.model.element.TypeElement}s and the fully qualified name
 * of the configuration class, which means that we can easily get the DTO class from the configuration class
 * to generate a nice error message.
 * <p>
 * Unfortunately, because the referenced class might not exist as an actual class yet, we can't always
 * use the {@link GeneratedConfig#source()} property, so this class exists as a workaround.
 */

@Beta
@Singleton
public class GeneratedTypeCache {
    /**
     * Map of source type to generated type's qualified name
     */
    private final BiMap<TypeElement, String> generatedSpecs = HashBiMap.create();


    /**
     * @return the underlying mapping of source type to generated type's qualified name
     * <p>
     * This type is mutable and should be modified with care
     */
    public @NotNull BiMap<TypeElement, String> getGeneratedSpecs() {
        return generatedSpecs;
    }


    /**
     * Gets all the known DTO classes which generate a configuration class with the given qualified name
     *
     * @param name the qualified name of the configuration class
     * @return a set of all the DTO classes which generate the given configuration class
     */
    public @NotNull Set<TypeElement> getByName(@NotNull String name) {
        return generatedSpecs.entrySet()
                .stream()
                .filter(entry -> entry.getValue().contains(name) || name.contains(entry.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }
}
