package me.bristermitten.mittenlib.annotations.ast;

import com.squareup.javapoet.ClassName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * List of hierarchical names
 *
 * @param parentClassName
 * @param parent
 */
public record ASTParentReference(
        @NotNull ClassName parentClassName,
        @Nullable ASTParentReference parent
) {
}
