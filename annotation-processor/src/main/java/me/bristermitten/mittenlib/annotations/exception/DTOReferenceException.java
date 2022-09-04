package me.bristermitten.mittenlib.annotations.exception;

import me.bristermitten.mittenlib.annotations.config.GeneratedTypeCache;
import me.bristermitten.mittenlib.annotations.util.Stringify;

import javax.annotation.Nullable;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;
import java.util.Optional;

/**
 * Thrown when a DTO class references an invalid type
 */
public class DTOReferenceException extends RuntimeException {
    private final transient TypeMirror typeUsed;
    private final transient GeneratedTypeCache typeCache;

    private final @Nullable Class<?> replaceWith;
    @Nullable
    private final transient Element source;

    /**
     * Create a new DTOReferenceException
     *
     * @param typeUsed    The invalid type that was uses
     * @param typeCache   The type cache, used for generating the error message
     * @param replaceWith The type to replace the invalid type with, if known
     * @param source      The source element (i.e. the element referencing the invalid type), if known
     */
    public DTOReferenceException(TypeMirror typeUsed, GeneratedTypeCache typeCache, @Nullable Class<?> replaceWith, @Nullable Element source) {
        this.typeUsed = typeUsed;
        this.typeCache = typeCache;
        this.replaceWith = replaceWith;
        this.source = source;
    }

    @Override
    public String getMessage() {
        String typesReplaceWith;
        if (replaceWith != null) {
            typesReplaceWith = replaceWith.getName();
        } else {
            var types = typeCache.getByName(typeUsed.toString());
            if (types.isEmpty()) {
                return "Unknown type %s".formatted(typeUsed);
            }

            typesReplaceWith = types.size() == 1
                    ? Stringify.stringify(types.iterator().next())
                    : "any of " + types.stream().map(Stringify::stringify).toList();
        }


        return """
                You seem to be using a generated type in a DTO.
                This results in weird behaviour and so is not allowed.
                You should replace %s with %s.
                This issue occurred in %s.
                """
                .formatted(typeUsed, typesReplaceWith, Optional.ofNullable(source).map(Stringify::stringify).orElse("Unknown Location"));
    }

}
