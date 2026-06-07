package me.bristermitten.mittenlib.annotations.parser;

import com.squareup.javapoet.ClassName;
import io.toolisticon.aptk.tools.MessagerUtils;
import me.bristermitten.mittenlib.annotations.ast.AbstractConfigStructure;
import me.bristermitten.mittenlib.annotations.ast.ConfigTypeSource.ClassConfigTypeSource;
import me.bristermitten.mittenlib.annotations.ast.Property;
import me.bristermitten.mittenlib.annotations.ast.ValidationConstraint;
import me.bristermitten.mittenlib.config.validation.Validator;

import javax.inject.Inject;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;

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
                Optional<ExecutableElement> noArgConstructor = element.getEnclosedElements().stream()
                        .filter(e -> e.getKind() == ElementKind.CONSTRUCTOR)
                        .map(ExecutableElement.class::cast)
                        .filter(c -> c.getParameters().isEmpty())
                        .findFirst();
                if (noArgConstructor.isEmpty() || noArgConstructor.get().getModifiers().contains(Modifier.PRIVATE)) {
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
                        TypeElement validatorElement = elements.getTypeElement(Validator.class.getName());
                        TypeElement customElement = elements.getTypeElement(val.canonicalName());
                        if (customElement == null) {
                            MessagerUtils.error(property.source().element(),
                                    "Custom validator class " + val.canonicalName() + " not found");
                            success = false;
                        } else {
                            TypeMirror boxedType = type.getKind().isPrimitive() ? types.boxedClass((PrimitiveType) type).asType() : type;
                            TypeMirror wildcard = types.getWildcardType(null, boxedType); // <?>
                            TypeMirror expectedValidatorType = types.getDeclaredType(validatorElement, wildcard); // Validator<?>
                            if (!types.isAssignable(customElement.asType(), expectedValidatorType)) {
                                MessagerUtils.error(property.source().element(),
                                        ConfigVerificationErrors.CONSTRAINT_TYPE_MISMATCH,
                                        "@ValidateWith(" + val.simpleName() + ".class)", type.toString(), "Validator compatible with " + type);
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
        Element element = types.asElement(type);
        if (element instanceof TypeElement typeElement) {
            String typeStr = typeElement.getQualifiedName().toString();
            return typeStr.equals(Byte.class.getName()) ||
                   typeStr.equals(Short.class.getName()) ||
                   typeStr.equals(Integer.class.getName()) ||
                   typeStr.equals(Long.class.getName()) ||
                   typeStr.equals(Float.class.getName()) ||
                   typeStr.equals(Double.class.getName()) ||
                   typeStr.equals(BigInteger.class.getName()) ||
                   typeStr.equals(BigDecimal.class.getName());
        }
        return false;
    }

    private boolean isStringType(TypeMirror type) {
        Element element = types.asElement(type);
        if (element instanceof TypeElement typeElement) {
            String typeStr = typeElement.getQualifiedName().toString();
            return typeStr.equals(String.class.getName()) ||
                   typeStr.equals(CharSequence.class.getName());
        }
        return false;
    }
}
