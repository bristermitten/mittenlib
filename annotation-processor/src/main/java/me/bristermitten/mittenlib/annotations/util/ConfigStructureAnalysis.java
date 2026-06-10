package me.bristermitten.mittenlib.annotations.util;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import java.util.HashSet;
import java.util.Set;
import javax.lang.model.type.TypeMirror;
import me.bristermitten.mittenlib.annotations.ast.AbstractConfigStructure;
import me.bristermitten.mittenlib.annotations.compile.ConfigNameCache;

@Singleton
public class ConfigStructureAnalysis {
    private final ConfigNameCache cache;

    @Inject
    public ConfigStructureAnalysis(ConfigNameCache cache) {
        this.cache = cache;
    }

    /**
     * Returns whether this {@link AbstractConfigStructure} is dynamically initializable, i.e.
     * every property is either nullable or has a default value.
     */
    public boolean isDynamicallyInitializable(AbstractConfigStructure structure) {
        return isDynamicallyInitializable(structure, new HashSet<>());
    }

    /**
     * Returns whether this {@link TypeMirror} is a configuration type that MittenLib knows how to initialize.
     */
    public boolean isTypeInitializable(TypeMirror type) {
        return isTypeInitializable(type, new HashSet<>());
    }

    private boolean isDynamicallyInitializable(AbstractConfigStructure structure, Set<ClassName> visited) {
        if (!visited.add(structure.name())) {
            return true; // Cycle detected, assume true to allow other properties to decide
        }
        return structure.properties().stream()
                .allMatch(p -> p.settings().hasDefaultValue()
                        || p.settings().isNullable()
                        || isTypeInitializable(p.propertyType(), visited));
    }

    private boolean isTypeInitializable(TypeMirror type, Set<ClassName> visited) {
        return cache.lookupAST(type)
                .map(structure -> isDynamicallyInitializable(structure, visited))
                .orElse(false);
    }

    public boolean needsValidation(AbstractConfigStructure structure) {
        return structure.properties().stream()
                .anyMatch(p -> !p.settings().constraints().isEmpty()
                        || !p.settings().elementConstraints().isEmpty()
                        || !p.settings().keyConstraints().isEmpty()
                        || (!TypeName.get(p.propertyType()).isPrimitive()
                                && !p.settings().isNullable()));
    }
}
