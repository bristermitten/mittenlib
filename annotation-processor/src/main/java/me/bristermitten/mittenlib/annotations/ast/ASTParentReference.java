package me.bristermitten.mittenlib.annotations.ast;

import com.squareup.javapoet.ClassName;
import org.jspecify.annotations.Nullable;


/**
 * List of hierarchical names
 *
 * @param parentClassName the class name of the parent
 * @param parent          parent reference, null if this is the root
 */
public record ASTParentReference(
        ClassName parentClassName,
        @Nullable ASTParentReference parent
) {
}
