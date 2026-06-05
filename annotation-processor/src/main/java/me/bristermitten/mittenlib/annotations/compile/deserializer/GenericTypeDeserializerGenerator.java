package me.bristermitten.mittenlib.annotations.compile.deserializer;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import io.toolisticon.aptk.tools.TypeMirrorWrapper;
import io.toolisticon.aptk.tools.corematcher.AptkCoreMatchers;
import io.toolisticon.aptk.tools.wrapper.ElementWrapper;
import io.toolisticon.aptk.tools.wrapper.TypeElementWrapper;
import me.bristermitten.mittenlib.annotations.ast.CustomDeserializerInfo;
import me.bristermitten.mittenlib.annotations.ast.Property;
import me.bristermitten.mittenlib.annotations.compile.ConfigurationClassNameGenerator;
import me.bristermitten.mittenlib.annotations.compile.MethodNames;
import me.bristermitten.mittenlib.annotations.parser.CustomDeserializers;
import me.bristermitten.mittenlib.annotations.util.TypesUtil;
import me.bristermitten.mittenlib.config.CollectionsUtils;
import me.bristermitten.mittenlib.config.DeserializationContext;
import org.jspecify.annotations.Nullable;

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
    private final MethodNames methodNames;
    private final CustomDeserializers customDeserializers;

    @Inject
    GenericTypeDeserializerGenerator(
            TypesUtil typesUtil,
            ConfigurationClassNameGenerator configurationClassNameGenerator,
            MethodNames methodNames,
            CustomDeserializers customDeserializers) {
        this.typesUtil = typesUtil;
        this.configurationClassNameGenerator = configurationClassNameGenerator;
        this.methodNames = methodNames;
        this.customDeserializers = customDeserializers;
    }

    private CodeBlock getDeserializationFunctionReference(CustomDeserializerInfo info) {
        if (info.isStatic()) {
            return CodeBlock.of("$T::deserialize", info.deserializerClass());
        }
        throw new IllegalArgumentException("idk non-static is hard");
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

    private Optional<MethodSpec> handleListType(MethodSpec.Builder builder,
                                                TypeMirrorWrapper wrappedElementType,
                                                String fromMapName) {
        var listType = wrappedElementType.getTypeArguments().getFirst();
        Optional<CustomDeserializerInfo> optional = customDeserializers.getCustomDeserializer(listType);

        if (optional.isPresent()) {
            CustomDeserializerInfo info = optional.get();
            CodeBlock deserializationFunction = getDeserializationFunctionReference(info);
            builder.addStatement("return $T.deserializeList($L, context, $L)",
                    CollectionsUtils.class, fromMapName, deserializationFunction);
            return Optional.of(builder.build());
        }

        if (typesUtil.isConfigType(listType)) {
            TypeName listTypeName = configurationClassNameGenerator.getConfigClassName(listType, null);
            var deserializeCodeBlock = CodeBlock.of("$T::$L", listTypeName,
                    methodNames.getDeserializeMethodName(listTypeName));

            builder.addStatement("return $T.deserializeList($L, context, $L)", CollectionsUtils.class, fromMapName, deserializeCodeBlock);
            return Optional.of(builder.build());
        }

        return Optional.empty();
    }

    private Optional<MethodSpec> handleMapType(MethodSpec.Builder builder,
                                               TypeMirrorWrapper wrappedElementType, String fromMapName) {
        var arguments = wrappedElementType.getTypeArguments();
        var keyType = arguments.get(0);
        var valueType = arguments.get(1);

        Optional<CustomDeserializerInfo> optional = customDeserializers.getCustomDeserializer(valueType);
        if (optional.isPresent()) {
            CustomDeserializerInfo info = optional.get();
            CodeBlock deserializationFunction = getDeserializationFunctionReference(info);
            builder.addStatement("return $T.deserializeMap($L, context, $L)",
                    CollectionsUtils.class, fromMapName, deserializationFunction);
            return Optional.of(builder.build());
        }

        if (typesUtil.isConfigType(valueType)) {
            TypeName mapTypeName = configurationClassNameGenerator.getConfigClassName(valueType, null);
            builder.addStatement("return $T.deserializeMap($T.class, $L, context, $T::$L)",
                    CollectionsUtils.class,
                    typesUtil.getSafeType(keyType),
                    fromMapName,
                    mapTypeName,
                    methodNames.getDeserializeMethodName(mapTypeName));
            return Optional.of(builder.build());
        }

        return Optional.empty();
    }
}
