package me.bristermitten.mittenlib.annotations.parser;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import io.toolisticon.aptk.compilermessage.api.DeclareCompilerMessage;
import io.toolisticon.aptk.compilermessage.api.DeclareCompilerMessageCodePrefix;
import io.toolisticon.aptk.tools.MessagerUtils;
import io.toolisticon.aptk.tools.TypeUtils;
import io.toolisticon.aptk.tools.wrapper.ExecutableElementWrapper;
import io.toolisticon.aptk.tools.wrapper.TypeElementWrapper;
import me.bristermitten.mittenlib.annotations.CustomDeserializerForWrapper;
import me.bristermitten.mittenlib.annotations.ast.CustomDeserializerInfo;
import me.bristermitten.mittenlib.config.DeserializationContext;
import me.bristermitten.mittenlib.config.extension.CustomDeserializer;
import me.bristermitten.mittenlib.config.extension.Fallback;
import me.bristermitten.mittenlib.util.Result;

import javax.inject.Singleton;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.Optional;

@Singleton
@DeclareCompilerMessageCodePrefix("CUSTOM_DESERIALIZER")
public class CustomDeserializers {
    private final Multimap<TypeName, CustomDeserializerInfo> deserializerInfoMultimap = HashMultimap.create();


    public void register(TypeName clazz, CustomDeserializerInfo info) {
        deserializerInfoMultimap.put(clazz, info);
    }

    public Optional<CustomDeserializerInfo> getCustomDeserializer(TypeMirror propertyType) {
        var fromMap = deserializerInfoMultimap.get(TypeName.get(propertyType));

        if (fromMap.isEmpty()) {
            return Optional.empty();
        }
        if (fromMap.size() > 1) {
            throw new IllegalArgumentException("Not sure how to handle multiple yet");
        }

        return Optional.of(fromMap.iterator().next());
    }


    @DeclareCompilerMessage(code = "001", enumValueName = "INVALID_STATIC_METHOD_SIGNATURE", message = "Custom deserializer method must be static and be of the signature Result<${0}> deserialize(DeserializationContext)")
    @DeclareCompilerMessage(code = "002", enumValueName = "UNSUPPORTED_NON_STATIC", message = "Non static custom deserializers aren't supported yet")
    public void registerCustomDeserializer(TypeElement customDeserializerType) {
        CustomDeserializerForWrapper deserializerTypeAnnotation = CustomDeserializerForWrapper.wrap(customDeserializerType);
        if (deserializerTypeAnnotation == null) {
            throw new IllegalArgumentException("CustomDeserializer must be annotated with @CustomDeserializerFor");
        }

        var implementsCustomDeserializer = TypeElementWrapper.wrap(customDeserializerType)
                .getAllInterfaces()
                .stream().anyMatch(i -> i.getQualifiedName().equals(CustomDeserializer.class.getCanonicalName()));

        Optional<ExecutableElementWrapper> deserializeMethodOpt = TypeElementWrapper.wrap(customDeserializerType)
                .getMethod("deserialize", DeserializationContext.class);

        if (implementsCustomDeserializer) {
            MessagerUtils.error(customDeserializerType, CustomDeserializersCompilerMessages.INVALID_STATIC_METHOD_SIGNATURE);
            return;
        }
        if (deserializeMethodOpt.isEmpty()) {
            throw new IllegalArgumentException("CustomDeserializer must implement CustomDeserializer or have a static method Result<T> deserialize(DeserializationContext)");
        }


        TypeMirror deserializerFor = deserializerTypeAnnotation.valueAsTypeMirror();

        if (deserializeMethodOpt.isPresent()) {
            ExecutableElementWrapper deserializeMethod = deserializeMethodOpt.get();

            if (!TypeUtils.TypeComparison.isTypeEqual(
                    deserializeMethod.getReturnType().unwrap(),
                    TypeUtils.Generics.createGenericType(Result.class,
                            TypeUtils.Generics.createGenericType(deserializerFor))
            )) {
                MessagerUtils.error(deserializeMethod.unwrap(), CustomDeserializersCompilerMessages.INVALID_STATIC_METHOD_SIGNATURE, deserializerFor);
                return;
            }
        }

        var isStatic = true; // TODO

        var isFallback = customDeserializerType.getAnnotation(Fallback.class) != null;

        var customDeserializerInfo = new CustomDeserializerInfo(
                customDeserializerType,
                isStatic,
                isFallback,
                false // TODO
        );

        register(ClassName.get(deserializerFor), customDeserializerInfo);

    }
}
