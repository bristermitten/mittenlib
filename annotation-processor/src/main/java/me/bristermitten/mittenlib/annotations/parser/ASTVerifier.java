package me.bristermitten.mittenlib.annotations.parser;

import com.google.inject.Inject;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.TypeName;
import io.toolisticon.aptk.tools.MessagerUtils;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import me.bristermitten.mittenlib.annotations.ast.AbstractConfigStructure;
import me.bristermitten.mittenlib.annotations.ast.ConfigTypeSource.ClassConfigTypeSource;
import me.bristermitten.mittenlib.annotations.ast.Property;
import me.bristermitten.mittenlib.annotations.ast.ValidationConstraint;
import me.bristermitten.mittenlib.annotations.compile.SerializationCodeGenerator;
import me.bristermitten.mittenlib.annotations.util.ConfigStructureAnalysis;
import me.bristermitten.mittenlib.annotations.util.TypesUtil;
import me.bristermitten.mittenlib.config.validation.Validator;

/** Inspects the AST and sends errors/warnings for invalid setups */
public class ASTVerifier {
    private final Types types;
    private final Elements elements;
    private final SerializationCodeGenerator serializationCodeGenerator;
    private final ConfigStructureAnalysis configStructureAnalysis;
    private final TypesUtil typesUtil;

    @Inject
    public ASTVerifier(
            Types types,
            Elements elements,
            SerializationCodeGenerator serializationCodeGenerator,
            ConfigStructureAnalysis configStructureAnalysis,
            TypesUtil typesUtil) {
        this.types = types;
        this.elements = elements;
        this.serializationCodeGenerator = serializationCodeGenerator;
        this.configStructureAnalysis = configStructureAnalysis;
        this.typesUtil = typesUtil;
    }

    public boolean verify(AbstractConfigStructure structure) {
        boolean success = true;
        if (structure instanceof AbstractConfigStructure.Union union) {
            TypeMirror unionType = union.source().element().asType();
            if (!union.properties().isEmpty()) {
                // if there are some properties, all alternatives must extend the union
                for (AbstractConfigStructure alternative : union.alternatives()) {
                    if (alternative.source().parents().stream().noneMatch(t -> types.isSameType(t, unionType))) {
                        MessagerUtils.error(
                                alternative.source().element(),
                                ConfigVerificationErrors.UNION_ALTERNATIVE_NOT_EXTENDING_UNION,
                                union.source().element());
                        success = false;
                    }
                }
            }
        }

        if (structure.source() instanceof ClassConfigTypeSource classSource) {
            boolean hasAnyDefault =
                    structure.properties().stream().anyMatch(p -> p.settings().hasDefaultValue());
            if (hasAnyDefault) {
                TypeElement element = classSource.element();
                Optional<ExecutableElement> noArgConstructor = element.getEnclosedElements().stream()
                        .filter(e -> e.getKind() == ElementKind.CONSTRUCTOR)
                        .map(ExecutableElement.class::cast)
                        .filter(c -> c.getParameters().isEmpty())
                        .findFirst();
                if (noArgConstructor.isEmpty()
                        || noArgConstructor.get().getModifiers().contains(Modifier.PRIVATE)) {
                    MessagerUtils.error(
                            element,
                            ConfigVerificationErrors.CLASS_DTO_MISSING_NO_ARG_CONSTRUCTOR,
                            element.getSimpleName());
                    success = false;
                }
            }
        }

        if (!serializationCodeGenerator.isSerializationSupported(structure)) {
            var unsupported = serializationCodeGenerator.getUnsupportedSerializationProperties(structure);
            if (structure.settings().config().requireSerialization()) {
                MessagerUtils.error(
                        structure.source().element(),
                        ConfigVerificationErrors.SERIALIZATION_NOT_SUPPORTED,
                        String.join(", ", unsupported));
                success = false;
            } else {
                MessagerUtils.warning(
                        structure.source().element(),
                        ConfigVerificationErrors.SERIALIZATION_NOT_SUPPORTED_WARNING,
                        String.join(", ", unsupported));
            }
        }

        // Error/warn if a config with a @Source is not dynamically initializable
        if (structure.settings().source() != null && !configStructureAnalysis.isDynamicallyInitializable(structure)) {
            var missingDefaults = structure.properties().stream()
                    .filter(p -> !p.settings().hasDefaultValue()
                            && !p.settings().isNullable()
                            && !configStructureAnalysis.isTypeInitializable(p.propertyType()))
                    .map(Property::name)
                    .toList();
            if (!missingDefaults.isEmpty()) {
                if (structure.settings().config().requireDynamicInitialization()) {
                    MessagerUtils.error(
                            structure.source().element(),
                            ConfigVerificationErrors.NOT_DYNAMICALLY_INITIALIZABLE,
                            structure.name().simpleName(),
                            String.join(", ", missingDefaults),
                            structure.settings().source().value());
                    success = false;
                } else {
                    MessagerUtils.warning(
                            structure.source().element(),
                            ConfigVerificationErrors.NOT_DYNAMICALLY_INITIALIZABLE,
                            structure.name().simpleName(),
                            String.join(", ", missingDefaults),
                            structure.settings().source().value());
                }
            }
        }

        for (Property property : structure.properties()) {
            TypeMirror type = property.propertyType();
            boolean isNumeric = isNumericType(type);
            boolean isString = isStringType(type);

            for (ValidationConstraint constraint : property.settings().constraints()) {
                if (!verifyConstraint(property, constraint, type, isNumeric, isString)) {
                    success = false;
                }
            }

            if (type instanceof DeclaredType declaredType
                    && !declaredType.getTypeArguments().isEmpty()) {
                List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
                if (typesUtil.isCollection(type)) {
                    TypeMirror elementType = typeArguments.get(0);
                    boolean isElementNumeric = isNumericType(elementType);
                    boolean isElementString = isStringType(elementType);
                    for (ValidationConstraint constraint : property.settings().elementConstraints()) {
                        if (!verifyConstraint(property, constraint, elementType, isElementNumeric, isElementString)) {
                            success = false;
                        }
                    }
                } else if (typesUtil.isMap(type) && typeArguments.size() >= 2) {
                    TypeMirror keyType = typeArguments.get(0);
                    boolean isKeyNumeric = isNumericType(keyType);
                    boolean isKeyString = isStringType(keyType);
                    for (ValidationConstraint constraint : property.settings().keyConstraints()) {
                        if (!verifyConstraint(property, constraint, keyType, isKeyNumeric, isKeyString)) {
                            success = false;
                        }
                    }

                    TypeMirror valType = typeArguments.get(1);
                    boolean isValNumeric = isNumericType(valType);
                    boolean isValString = isStringType(valType);
                    for (ValidationConstraint constraint : property.settings().elementConstraints()) {
                        if (!verifyConstraint(property, constraint, valType, isValNumeric, isValString)) {
                            success = false;
                        }
                    }
                }
            }
        }

        return success;
    }

    private boolean verifyConstraint(
            Property property, ValidationConstraint constraint, TypeMirror type, boolean isNumeric, boolean isString) {
        boolean success = true;
        String typeNameStr = TypeName.get(type).toString();
        switch (constraint) {
            case ValidationConstraint.Positive() -> {
                if (!isNumeric) {
                    MessagerUtils.error(
                            property.source().element(),
                            ConfigVerificationErrors.CONSTRAINT_TYPE_MISMATCH,
                            "@Positive",
                            typeNameStr,
                            "numeric type");
                    success = false;
                }
            }
            case ValidationConstraint.Negative() -> {
                if (!isNumeric) {
                    MessagerUtils.error(
                            property.source().element(),
                            ConfigVerificationErrors.CONSTRAINT_TYPE_MISMATCH,
                            "@Negative",
                            typeNameStr,
                            "numeric type");
                    success = false;
                }
            }
            case ValidationConstraint.Min(double val) -> {
                if (!isNumeric) {
                    MessagerUtils.error(
                            property.source().element(),
                            ConfigVerificationErrors.CONSTRAINT_TYPE_MISMATCH,
                            "@Min",
                            typeNameStr,
                            "numeric type");
                    success = false;
                }
            }
            case ValidationConstraint.Max(double val) -> {
                if (!isNumeric) {
                    MessagerUtils.error(
                            property.source().element(),
                            ConfigVerificationErrors.CONSTRAINT_TYPE_MISMATCH,
                            "@Max",
                            typeNameStr,
                            "numeric type");
                    success = false;
                }
            }
            case ValidationConstraint.Range(double min, double max) -> {
                if (!isNumeric) {
                    MessagerUtils.error(
                            property.source().element(),
                            ConfigVerificationErrors.CONSTRAINT_TYPE_MISMATCH,
                            "@Range",
                            typeNameStr,
                            "numeric type");
                    success = false;
                }
            }
            case ValidationConstraint.NotBlank() -> {
                if (!isString) {
                    MessagerUtils.error(
                            property.source().element(),
                            ConfigVerificationErrors.CONSTRAINT_TYPE_MISMATCH,
                            "@NotBlank",
                            typeNameStr,
                            "String or CharSequence");
                    success = false;
                }
            }
            case ValidationConstraint.Custom(ClassName val) -> {
                TypeElement validatorElement = elements.getTypeElement(Validator.class.getName());
                TypeElement customElement = elements.getTypeElement(val.canonicalName());
                if (customElement == null) {
                    MessagerUtils.error(
                            property.source().element(),
                            "Custom validator class " + val.canonicalName() + " not found");
                    success = false;
                } else {
                    TypeMirror boxedType = type.getKind().isPrimitive()
                            ? types.boxedClass((PrimitiveType) type).asType()
                            : type;
                    TypeMirror wildcard = types.getWildcardType(null, boxedType); // <?>
                    TypeMirror expectedValidatorType =
                            types.getDeclaredType(validatorElement, wildcard); // Validator<?>
                    if (!types.isAssignable(customElement.asType(), expectedValidatorType)) {
                        MessagerUtils.error(
                                property.source().element(),
                                ConfigVerificationErrors.CONSTRAINT_TYPE_MISMATCH,
                                "@ValidateWith(" + val.simpleName() + ".class)",
                                typeNameStr,
                                "Validator compatible with " + typeNameStr);
                        success = false;
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
            return typeStr.equals(Byte.class.getName())
                    || typeStr.equals(Short.class.getName())
                    || typeStr.equals(Integer.class.getName())
                    || typeStr.equals(Long.class.getName())
                    || typeStr.equals(Float.class.getName())
                    || typeStr.equals(Double.class.getName())
                    || typeStr.equals(BigInteger.class.getName())
                    || typeStr.equals(BigDecimal.class.getName());
        }
        return false;
    }

    private boolean isStringType(TypeMirror type) {
        Element element = types.asElement(type);
        if (element instanceof TypeElement typeElement) {
            String typeStr = typeElement.getQualifiedName().toString();
            return typeStr.equals(String.class.getName()) || typeStr.equals(CharSequence.class.getName());
        }
        return false;
    }
}
