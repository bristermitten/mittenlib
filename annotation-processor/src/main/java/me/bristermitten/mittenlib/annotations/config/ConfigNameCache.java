package me.bristermitten.mittenlib.annotations.config;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import io.toolisticon.aptk.tools.TypeMirrorWrapper;
import me.bristermitten.mittenlib.annotations.ast.AbstractConfigStructure;
import org.jetbrains.annotations.NotNull;

import javax.inject.Singleton;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A cache for AbstractConfigStructure objects indexed by their class names.
 * This class provides lookup methods for different types of names (ClassName, TypeName, TypeMirror)
 * to efficiently retrieve cached configuration structures.
 */
@Singleton
public class ConfigNameCache {
    private final Map<ClassName, AbstractConfigStructure> astCache = new HashMap<>();

    /**
     * Looks up an AbstractConfigStructure by its ClassName.
     *
     * @param name The class name to look up
     * @return An Optional containing the AbstractConfigStructure if found, or empty if not found
     */
    public @NotNull Optional<AbstractConfigStructure> lookupAST(ClassName name) {
        return Optional.ofNullable(astCache.get(name));
    }

    /**
     * Looks up an AbstractConfigStructure by its TypeName.
     * This method only works with TypeNames that are ClassNames.
     *
     * @param name The type name to look up
     * @return An Optional containing the AbstractConfigStructure if found, or empty if not found or if the TypeName is not a ClassName
     */
    public @NotNull Optional<AbstractConfigStructure> lookupAST(TypeName name) {
        if (name instanceof ClassName cn) {
            return Optional.ofNullable(astCache.get(cn));
        }
        return Optional.empty();
    }

    /**
     * Looks up an AbstractConfigStructure by its TypeMirror.
     * This method only works with declared types.
     *
     * @param mirror The type mirror to look up
     * @return An Optional containing the AbstractConfigStructure if found, or empty if not found or if the TypeMirror is not a declared type
     */
    public Optional<AbstractConfigStructure> lookupAST(@NotNull TypeMirror mirror) {
        if (mirror.getKind() != TypeKind.DECLARED) {
            return Optional.empty();
        }
        return lookupAST(ClassName.bestGuess(TypeMirrorWrapper.wrap(mirror)
                .getQualifiedName()));
    }

    /**
     * Adds an AbstractConfigStructure to the cache, indexed by its name.
     * If an entry with the same name already exists, it will be replaced.
     *
     * @param ast The AbstractConfigStructure to add to the cache
     */
    public void put(@NotNull AbstractConfigStructure ast) {
        astCache.put(ast.name(), ast);
    }
}
