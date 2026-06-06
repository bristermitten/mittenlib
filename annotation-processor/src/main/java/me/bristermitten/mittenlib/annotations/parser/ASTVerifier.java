package me.bristermitten.mittenlib.annotations.parser;

import io.toolisticon.aptk.tools.MessagerUtils;
import me.bristermitten.mittenlib.annotations.ast.AbstractConfigStructure;
import me.bristermitten.mittenlib.annotations.ast.ConfigTypeSource.ClassConfigTypeSource;

import javax.inject.Inject;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
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

    public boolean verify(AbstractConfigStructure structure) {
        boolean success = true;
        if (structure instanceof AbstractConfigStructure.Union union) {
            TypeMirror unionType = union.source().element().asType();
            if (!union.properties().isEmpty()) {
                // if there are some properties, all alternatives must extend the union
                for (AbstractConfigStructure alternative : union.alternatives()) {
                    if (alternative.source().parents().stream().noneMatch(t -> types.isSameType(t, unionType))) {
                        MessagerUtils.error(alternative.source().element(),
                                ConfigVerificationErrors.UNION_ALTERNATIVE_NOT_EXTENDING_UNION,
                                union.source().element());
                        success = false;
                    }
                }
            }
        }
        
        if (structure.source() instanceof ClassConfigTypeSource classSource) {
            boolean hasAnyDefault = structure.properties().stream()
                    .anyMatch(p -> p.settings().hasDefaultValue());
            if (hasAnyDefault) {
                TypeElement element = classSource.element();
                boolean hasNoArgConstructor = element.getEnclosedElements().stream()
                        .filter(e -> e.getKind() == ElementKind.CONSTRUCTOR)
                        .map(ExecutableElement.class::cast)
                        .anyMatch(c -> c.getParameters().isEmpty());
                if (!hasNoArgConstructor) {
                    MessagerUtils.error(element,
                            ConfigVerificationErrors.CLASS_DTO_MISSING_NO_ARG_CONSTRUCTOR,
                            element.getSimpleName());
                    success = false;
                }
            }
        }
        return success;
    }
}
