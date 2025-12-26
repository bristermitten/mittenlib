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
            throw new IllegalArgumentException(String.format("""
                    
                    ╔════════════════════════════════════════════════════════════════════════════════╗
                    ║                  MULTIPLE CUSTOM DESERIALIZERS FOUND                           ║
                    ╚════════════════════════════════════════════════════════════════════════════════╝
                    
                    Multiple custom deserializers were found for the same type.
                    
                    ┌─ What to do:
                    │
                    │  Ensure only one custom deserializer is registered per type.
                    │  Remove or consolidate duplicate @CustomDeserializerFor annotations.
                    │
                    └─ Note: This is a current limitation that may be addressed in future versions.
                    
                    ════════════════════════════════════════════════════════════════════════════════
                    """));
        }

        return Optional.of(fromMap.iterator().next());
    }


    @DeclareCompilerMessage(code = "001", enumValueName = "INVALID_STATIC_METHOD_SIGNATURE", message = "Custom deserializer method must be static and be of the signature Result<${0}> deserialize(DeserializationContext)")
    @DeclareCompilerMessage(code = "002", enumValueName = "UNSUPPORTED_NON_STATIC", message = "Non static custom deserializers aren't supported yet")
    public void registerCustomDeserializer(TypeElement customDeserializerType) {
        CustomDeserializerForWrapper deserializerTypeAnnotation = CustomDeserializerForWrapper.wrap(customDeserializerType);
        if (deserializerTypeAnnotation == null) {
            throw new IllegalArgumentException(String.format("""
                    
                    ╔════════════════════════════════════════════════════════════════════════════════╗
                    ║                  MISSING @CustomDeserializerFor ANNOTATION                     ║
                    ╚════════════════════════════════════════════════════════════════════════════════╝
                    
                    A custom deserializer class must be annotated with @CustomDeserializerFor.
                    
                    Type: %s
                    
                    ┌─ What to do:
                    │
                    │  Add the @CustomDeserializerFor annotation to specify the target type:
                    │
                    │  @CustomDeserializerFor(MyType.class)
                    │  public class MyTypeDeserializer {
                    │      public static Result<MyType> deserialize(DeserializationContext context) {
                    │          // Your deserialization logic
                    │      }
                    │  }
                    │
                    └─ Note: The annotation tells the processor which type this deserializer handles.
                    
                    ════════════════════════════════════════════════════════════════════════════════
                    """, customDeserializerType.getQualifiedName()));
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
            throw new IllegalArgumentException(String.format("""
                    
                    ╔════════════════════════════════════════════════════════════════════════════════╗
                    ║                  INVALID CUSTOM DESERIALIZER STRUCTURE                         ║
                    ╚════════════════════════════════════════════════════════════════════════════════╝
                    
                    A custom deserializer must either:
                    1. Implement the CustomDeserializer interface, OR
                    2. Have a static method: Result<T> deserialize(DeserializationContext)
                    
                    Type: %s
                    
                    ┌─ What to do:
                    │
                    │  Option 1 - Static method approach (recommended):
                    │
                    │  @CustomDeserializerFor(MyType.class)
                    │  public class MyTypeDeserializer {
                    │      public static Result<MyType> deserialize(DeserializationContext context) {
                    │          // Your deserialization logic
                    │          return Result.ok(myValue);
                    │      }
                    │  }
                    │
                    │  Option 2 - Interface approach:
                    │
                    │  @CustomDeserializerFor(MyType.class)
                    │  public class MyTypeDeserializer implements CustomDeserializer<MyType> {
                    │      // Implement interface methods
                    │  }
                    │
                    └─ Note: The static method approach is preferred for simplicity.
                    
                    ════════════════════════════════════════════════════════════════════════════════
                    """, customDeserializerType.getQualifiedName()));
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
