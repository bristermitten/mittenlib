package me.bristermitten.mittenlib.annotations.parser

import com.google.inject.Singleton
import com.palantir.javapoet.TypeName
import io.toolisticon.aptk.tools.MessagerUtils
import io.toolisticon.aptk.tools.TypeUtils
import io.toolisticon.aptk.tools.wrapper.ExecutableElementWrapper
import io.toolisticon.aptk.tools.wrapper.TypeElementWrapper
import java.util.Optional
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.`type`.TypeMirror
import me.bristermitten.mittenlib.annotations.CustomDeserializerForWrapper
import me.bristermitten.mittenlib.annotations.ast.CustomDeserializerInfo
import me.bristermitten.mittenlib.config.DeserializationContext
import me.bristermitten.mittenlib.config.extension.CustomDeserializer
import me.bristermitten.mittenlib.config.extension.Fallback
import me.bristermitten.mittenlib.util.Result
import scala.jdk.CollectionConverters.*

@Singleton
class CustomDeserializers extends CustomInfoRegistry[CustomDeserializerInfo]:

  def registerCustomDeserializer(customDeserializerType: TypeElement): Unit =
    val deserializerTypeAnnotation =
      CustomDeserializerForWrapper.wrap(customDeserializerType)
    if (deserializerTypeAnnotation == null) {
      MessagerUtils.error(
        customDeserializerType,
        "CustomDeserializer must be annotated with @CustomDeserializerFor"
      )
      return
    }

    val implementsCustomDeserializer = TypeElementWrapper
      .wrap(customDeserializerType)
      .getAllInterfaces
      .asScala
      .exists(i =>
        i.getQualifiedName
          .equals(classOf[CustomDeserializer[?]].getCanonicalName)
      )

    val deserializeMethodOpt = TypeElementWrapper
      .wrap(customDeserializerType)
      .getMethod("deserialize", classOf[DeserializationContext])

    if (!implementsCustomDeserializer && deserializeMethodOpt.isEmpty) {
      MessagerUtils.error(
        customDeserializerType,
        "CustomDeserializer must implement CustomDeserializer or have a static method Result<T> deserialize(DeserializationContext)"
      )
      return
    }

    val deserializerFor = deserializerTypeAnnotation.valueAsTypeMirror()

    if (deserializeMethodOpt.isPresent) {
      val deserializeMethod = deserializeMethodOpt.get()
      if (
        !TypeUtils.TypeComparison.isTypeEqual(
          deserializeMethod.getReturnType.unwrap(),
          TypeUtils.Generics.createGenericType(
            classOf[Result[?]],
            TypeUtils.Generics.createGenericType(deserializerFor)
          )
        )
      ) {
        MessagerUtils.error(
          deserializeMethod.unwrap(),
          CustomDeserializersCompilerMessages.INVALID_STATIC_METHOD_SIGNATURE,
          deserializerFor
        )
        return
      }
    }

    val isStatic = !implementsCustomDeserializer &&
      deserializeMethodOpt.isPresent &&
      deserializeMethodOpt.get().unwrap().getModifiers.contains(Modifier.STATIC)

    if (!isStatic && !implementsCustomDeserializer) {
      MessagerUtils.error(
        customDeserializerType,
        CustomDeserializersCompilerMessages.UNSUPPORTED_NON_STATIC
      )
      return
    }

    val isFallback =
      customDeserializerType.getAnnotation(classOf[Fallback]) != null

    val customDeserializerInfo = CustomDeserializerInfo(
      customDeserializerType,
      isStatic,
      isFallback,
      false
    )

    register(TypeName.get(deserializerFor), customDeserializerInfo)
