package me.bristermitten.mittenlib.annotations.parser;

import io.toolisticon.aptk.tools.MessagerUtils;
import me.bristermitten.mittenlib.annotations.ast.AbstractConfigStructure;

import javax.inject.Inject;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

/**
 * Inspects the AST and sends errors/warnings for invalid setups
 */
public class ASTVerifier {
    private final Types types;

    @Inject
    public ASTVerifier(Types types) {
        this.types = types;
    }

    public void verify(AbstractConfigStructure structure) {
        if (structure instanceof AbstractConfigStructure.Union union) {
            TypeMirror unionType = union.source().element().asType();
            if (!union.properties().isEmpty()) {
                // if there are some properties, all alternatives must extend the union
                for (AbstractConfigStructure alternative : union.alternatives()) {
                    if (alternative.source().parents().stream().noneMatch(t -> types.isSameType(t, unionType))) {
                        MessagerUtils.error(alternative.source().element(),
                                ConfigVerificationErrors.UNION_ALTERNATIVE_NOT_EXTENDING_UNION,
                                union.source().element());
                    }
                }
            }
        }
    }
}
