package me.bristermitten.mittenlib.annotations.parser;

import com.squareup.javapoet.ClassName;
import io.toolisticon.aptk.tools.MessagerUtils;
import me.bristermitten.mittenlib.annotations.ast.AbstractConfigStructure;
import me.bristermitten.mittenlib.annotations.ast.ConfigTypeSource.ClassConfigTypeSource;
import me.bristermitten.mittenlib.annotations.ast.Property;
import me.bristermitten.mittenlib.annotations.ast.ValidationConstraint;

import javax.inject.Inject;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

/**
 * Inspects the AST and sends errors/warnings for invalid setups
 */
public class ASTVerifier {
    private final Types types;
    private final Elements elements;

    @Inject
    public ASTVerifier(Types types, Elements elements) {
        this.types = types;
        this.elements = elements;
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

        for (Property property : structure.properties()) {
            TypeMirror type = property.propertyType();
            boolean isNumeric = isNumericType(type);
            boolean isString = isStringType(type);

            for (ValidationConstraint constraint : property.settings().constraints()) {
                switch (constraint) {
                    case ValidationConstraint.Positive() -> {
                        if (!isNumeric) {
                            MessagerUtils.error(property.source().element(),
                                    ConfigVerificationErrors.CONSTRAINT_TYPE_MISMATCH,
                                    "@Positive", type.toString(), "numeric type");
                            success = false;
                        }
                    }
                    case ValidationConstraint.Negative() -> {
                        if (!isNumeric) {
                            MessagerUtils.error(property.source().element(),
                                    ConfigVerificationErrors.CONSTRAINT_TYPE_MISMATCH,
                                    "@Negative", type.toString(), "numeric type");
                            success = false;
                        }
                    }
                    case ValidationConstraint.Min(double val) -> {
                        if (!isNumeric) {
                            MessagerUtils.error(property.source().element(),
                                    ConfigVerificationErrors.CONSTRAINT_TYPE_MISMATCH,
                                    "@Min", type.toString(), "numeric type");
                            success = false;
                        }
                    }
                    case ValidationConstraint.Max(double val) -> {
                        if (!isNumeric) {
                            MessagerUtils.error(property.source().element(),
                                    ConfigVerificationErrors.CONSTRAINT_TYPE_MISMATCH,
                                    "@Max", type.toString(), "numeric type");
                            success = false;
                        }
                    }
                    case ValidationConstraint.Range(double min, double max) -> {
                        if (!isNumeric) {
                            MessagerUtils.error(property.source().element(),
                                    ConfigVerificationErrors.CONSTRAINT_TYPE_MISMATCH,
                                    "@Range", type.toString(), "numeric type");
                            success = false;
                        }
                    }
                    case ValidationConstraint.NotBlank() -> {
                        if (!isString) {
                            MessagerUtils.error(property.source().element(),
                                    ConfigVerificationErrors.CONSTRAINT_TYPE_MISMATCH,
                                    "@NotBlank", type.toString(), "String or CharSequence");
                            success = false;
                        }
                    }
                    case ValidationConstraint.Custom(ClassName val) -> {
                        TypeElement validatorElement = elements.getTypeElement("me.bristermitten.mittenlib.config.validation.Validator");
                        TypeElement customElement = elements.getTypeElement(val.canonicalName());
                        if (customElement == null) {
                            MessagerUtils.error(property.source().element(),
                                    "Custom validator class " + val.canonicalName() + " not found");
                            success = false;
                        } else {
                            TypeMirror boxedType = type.getKind().isPrimitive() ? types.boxedClass((javax.lang.model.type.PrimitiveType) type).asType() : type;
                            TypeMirror wildcard = types.getWildcardType(null, boxedType);
                            TypeMirror expectedValidatorType = types.getDeclaredType(validatorElement, wildcard);
                            if (!types.isAssignable(customElement.asType(), expectedValidatorType)) {
                                MessagerUtils.error(property.source().element(),
                                        ConfigVerificationErrors.CONSTRAINT_TYPE_MISMATCH,
                                        "@ValidateWith(" + val.simpleName() + ".class)", type.toString(), "Validator compatible with " + type.toString());
                                success = false;
                            }
                        }
                    }
                }
            }
        }

        return success;
    }

    private boolean isNumericType(TypeMirror type) {
        if (type.getKind().isPrimitive()) {
            return switch (type.getKind()) {
                case BYTE, SHORT, INT, LONG, FLOAT, DOUBLE -> true;
                default -> false;
            };
        }
        javax.lang.model.element.Element element = types.asElement(type);
        if (element instanceof TypeElement typeElement) {
            String typeStr = typeElement.getQualifiedName().toString();
            return typeStr.equals("java.lang.Byte") ||
                   typeStr.equals("java.lang.Short") ||
                   typeStr.equals("java.lang.Integer") ||
                   typeStr.equals("java.lang.Long") ||
                   typeStr.equals("java.lang.Float") ||
                   typeStr.equals("java.lang.Double") ||
                   typeStr.equals("java.math.BigInteger") ||
                   typeStr.equals("java.math.BigDecimal");
        }
        return false;
    }

    private boolean isStringType(TypeMirror type) {
        javax.lang.model.element.Element element = types.asElement(type);
        if (element instanceof TypeElement typeElement) {
            String typeStr = typeElement.getQualifiedName().toString();
            return typeStr.equals("java.lang.String") ||
                   typeStr.equals("java.lang.CharSequence");
        }
        return false;
    }
}
