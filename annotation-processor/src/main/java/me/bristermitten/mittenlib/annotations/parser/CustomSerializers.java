package me.bristermitten.mittenlib.annotations.parser;

import com.google.inject.Singleton;
import com.squareup.javapoet.ClassName;
import io.toolisticon.aptk.tools.MessagerUtils;
import io.toolisticon.aptk.tools.TypeUtils;
import io.toolisticon.aptk.tools.wrapper.ExecutableElementWrapper;
import io.toolisticon.aptk.tools.wrapper.TypeElementWrapper;
import me.bristermitten.mittenlib.annotations.ast.CustomSerializerInfo;
import me.bristermitten.mittenlib.config.SerializationContext;
import me.bristermitten.mittenlib.config.extension.CustomSerializer;
import me.bristermitten.mittenlib.config.extension.CustomSerializerFor;
import me.bristermitten.mittenlib.config.tree.DataTree;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import java.util.Optional;

@Singleton
public class CustomSerializers extends CustomInfoRegistry<CustomSerializerInfo> {

    public void registerCustomSerializer(TypeElement customSerializerType) {
        CustomSerializerFor annotation = customSerializerType.getAnnotation(CustomSerializerFor.class);
        if (annotation == null) {
            throw new IllegalArgumentException(
                    "CustomSerializer must be annotated with @CustomSerializerFor");
        }

        TypeMirror serializerFor;
        try {
            var ignored = annotation.value();
            throw new IllegalStateException("Expected MirroredTypeException");
        } catch (MirroredTypeException e) {
            serializerFor = e.getTypeMirror();
        }

        var implementsCustomSerializer =
                TypeElementWrapper.wrap(customSerializerType).getAllInterfaces().stream()
                        .anyMatch(i -> i.getQualifiedName().equals(CustomSerializer.class.getCanonicalName()));

        Optional<ExecutableElementWrapper> serializeMethodOpt =
                TypeElementWrapper.wrap(customSerializerType).getMethods().stream()
                        .filter(m -> m.getSimpleName().equals("serialize"))
                        .filter(m -> m.getParameters().size() == 2)
                        .filter(
                                m ->
                                        TypeUtils.TypeComparison.isTypeEqual(
                                                m.getParameters().getFirst().asType().unwrap(), serializerFor))
                        .filter(
                                m ->
                                        m.getParameters()
                                                .get(1)
                                                .asType()
                                                .toString()
                                                .equals(SerializationContext.class.getCanonicalName()))
                        .findFirst();

        if (serializeMethodOpt.isPresent()) {
            var method = serializeMethodOpt.get();
            if (!method.getReturnType().unwrap().toString().equals(DataTree.class.getCanonicalName())) {
                MessagerUtils.error(method.unwrap(), "Custom serializer method must return DataTree");
                return;
            }
        }

        if (!implementsCustomSerializer && serializeMethodOpt.isEmpty()) {
            throw new IllegalArgumentException(
                    "CustomSerializer must implement CustomSerializer or have a static method DataTree serialize(T, SerializationContext)");
        }

        boolean isStatic =
                !implementsCustomSerializer
                        && serializeMethodOpt.isPresent()
                        && serializeMethodOpt.get().unwrap().getModifiers().contains(Modifier.STATIC);

        if (!isStatic && !implementsCustomSerializer) {
            MessagerUtils.error(
                    customSerializerType, "Non static custom serializers must implement CustomSerializer");
            return;
        }

        var customSerializerInfo = new CustomSerializerInfo(customSerializerType, isStatic);

        register(ClassName.get(serializerFor), customSerializerInfo);
    }
}
