package me.bristermitten.mittenlib.annotations.compile.deserializer;

import com.google.gson.reflect.TypeToken;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import io.toolisticon.aptk.tools.TypeMirrorWrapper;
import io.toolisticon.aptk.tools.corematcher.AptkCoreMatchers;
import io.toolisticon.aptk.tools.wrapper.ElementWrapper;
import io.toolisticon.aptk.tools.wrapper.TypeElementWrapper;
import me.bristermitten.mittenlib.annotations.ast.CustomDeserializerInfo;
import me.bristermitten.mittenlib.annotations.ast.Property;
import me.bristermitten.mittenlib.annotations.compile.ConfigurationClassNameGenerator;
import me.bristermitten.mittenlib.annotations.parser.CustomDeserializers;
import me.bristermitten.mittenlib.annotations.util.TypesUtil;
import me.bristermitten.mittenlib.config.CollectionsUtils;
import me.bristermitten.mittenlib.util.Strings;

import javax.inject.Inject;
import javax.lang.model.type.TypeMirror;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Generates deserialization code for generic collection types (specifically {@link List} and {@link Map})
 * where the element or value types are custom configuration types.
 */
public class GenericTypeDeserializerGenerator {

    private final TypesUtil typesUtil;
    private final ConfigurationClassNameGenerator configurationClassNameGenerator;
    private final CustomDeserializers customDeserializers;

    @Inject
    GenericTypeDeserializerGenerator(
            TypesUtil typesUtil,
            ConfigurationClassNameGenerator configurationClassNameGenerator,
            CustomDeserializers customDeserializers) {
        this.typesUtil = typesUtil;
        this.configurationClassNameGenerator = configurationClassNameGenerator;
        this.customDeserializers = customDeserializers;
    }

    private CodeBlock getDeserializationFunctionReference(CustomDeserializerInfo info) {
        if (info.isStatic()) {
            return CodeBlock.of("$T::deserialize", info.deserializerClass());
        }
        String fieldName = Strings.uncapitalize(info.deserializerClass().getSimpleName().toString());
        return CodeBlock.of("this.$L", fieldName);
    }

    /**
     * Generates and appends deserialization logic for generic collection properties.
     *
     * @param builder            the method spec builder
     * @param property           the property being processed
     * @param wrappedElementType the wrapped property type mirror
     * @param elementType        the wrapped property type element
     * @return an optional method spec if handled successfully
     */
    public Optional<MethodSpec> handleGenericType(MethodSpec.Builder builder, Property property,
                                                   TypeMirrorWrapper wrappedElementType,
                                                   TypeElementWrapper elementType) {
        if (!hasNestedCustomDeserializerOrConfig(wrappedElementType.unwrap())) {
            return Optional.empty();
        }

        String canonicalName = wrappedElementType.erasure().getQualifiedName();
        ElementWrapper.wrap(property.source().element())
                .validate()
                .asError()
                .check($ -> AptkCoreMatchers.BY_RAW_TYPE
                        .getValidator()
                        .hasOneOf(elementType.unwrap(), List.class, Map.class))
                .validateAndIssueMessages();

        final String fromMapName = property.name() + "FromMap";

        if (canonicalName.equals(List.class.getName())) {
            return handleListType(builder, wrappedElementType, fromMapName);
        } else if (canonicalName.equals(Map.class.getName())) {
            return handleMapType(builder, wrappedElementType, fromMapName);
        } else {
            throw new IllegalStateException("Unexpected generic type: " + canonicalName);
        }
    }

    /**
     * Recursively checks if a type or any of its nested type arguments contains a custom deserializer
     * or a config type. Used to decide if custom recursive deserialization code needs to be generated.
     *
     * @param type the type to check
     * @return true if the type or any nested type argument contains a custom deserializer or a config type
     */
    private boolean hasNestedCustomDeserializerOrConfig(TypeMirror type) {
        if (customDeserializers.getCustomDeserializer(type).isPresent() || typesUtil.isConfigType(type)) {
            return true;
        }
        TypeMirrorWrapper wrapped = TypeMirrorWrapper.wrap(type);
        if (wrapped.hasTypeArguments()) {
            for (TypeMirror arg : wrapped.getTypeArguments()) {
                if (hasNestedCustomDeserializerOrConfig(arg)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Recursively generates a CodeBlock representing a {@link me.bristermitten.mittenlib.config.DeserializationFunction}
     * for the given type. Maps collections recursively and falls back to object mapper mapping for basic types.
     *
     * @param type  the type to generate a deserialization function for
     * @param depth the current nesting depth, used to generate unique context variable names (e.g. ctx0, ctx1)
     * @return a {@link CodeBlock} lambda expression {@code ctx -> ...}
     */
    private CodeBlock getDeserializationFunction(TypeMirror type, int depth) {
        TypeMirrorWrapper wrapped = TypeMirrorWrapper.wrap(type);

        // 1. Custom Deserializer
        Optional<CustomDeserializerInfo> customDeserializerOptional = customDeserializers.getCustomDeserializer(type);
        if (customDeserializerOptional.isPresent()) {
            return getDeserializationFunctionReference(customDeserializerOptional.get());
        }

        // 2. Config type
        if (typesUtil.isConfigType(type)) {
            String loaderField = configurationClassNameGenerator.getLoaderProviderFieldName(type);
            return CodeBlock.of("this.$L.get()", loaderField);
        }

        // 3. Generic collections (List, Map)
        if (wrapped.hasTypeArguments()) {
            String canonicalName = wrapped.erasure().getQualifiedName();
            String ctxVar = "ctx" + depth;
            if (canonicalName.equals(List.class.getName())) {
                TypeMirror elementType = wrapped.getTypeArguments().getFirst();
                CodeBlock innerFunction = getDeserializationFunction(elementType, depth + 1);
                return CodeBlock.of("$L -> $T.deserializeList($L.getData(), $L, $L)", ctxVar, CollectionsUtils.class, ctxVar, ctxVar, innerFunction);
            } else if (canonicalName.equals(Map.class.getName())) {
                var arguments = wrapped.getTypeArguments();
                TypeMirror keyType = arguments.get(0);
                TypeMirror valueType = arguments.get(1);
                CodeBlock innerFunction = getDeserializationFunction(valueType, depth + 1);
                return CodeBlock.of("$L -> $T.deserializeMap($T.class, $L.getData(), $L, $L)", ctxVar, CollectionsUtils.class, typesUtil.getSafeType(keyType), ctxVar, ctxVar, innerFunction);
            }
        }

        // 4. Basic fallback using ObjectMapper mapping
        String ctxVar = "ctx" + depth;
        return CodeBlock.of("$L -> $L.getMapper().map($L.getData(), new $T<$T>(){})", ctxVar, ctxVar, ctxVar, TypeToken.class, typesUtil.getBoxedType(type));
    }

    private Optional<MethodSpec> handleListType(MethodSpec.Builder builder,
                                                TypeMirrorWrapper wrappedElementType,
                                                String fromMapName) {
        var listType = wrappedElementType.getTypeArguments().getFirst();
        CodeBlock deserializationFunction = getDeserializationFunction(listType, 0);
        builder.addStatement("return $T.deserializeList($L, context, $L)",
                CollectionsUtils.class, fromMapName, deserializationFunction);
        return Optional.of(builder.build());
    }

    private Optional<MethodSpec> handleMapType(MethodSpec.Builder builder,
                                               TypeMirrorWrapper wrappedElementType, String fromMapName) {
        var arguments = wrappedElementType.getTypeArguments();
        var keyType = arguments.get(0);
        var valueType = arguments.get(1);

        CodeBlock deserializationFunction = getDeserializationFunction(valueType, 0);
        builder.addStatement("return $T.deserializeMap($T.class, $L, context, $L)",
                CollectionsUtils.class,
                typesUtil.getSafeType(keyType),
                fromMapName,
                deserializationFunction);
        return Optional.of(builder.build());
    }
}
