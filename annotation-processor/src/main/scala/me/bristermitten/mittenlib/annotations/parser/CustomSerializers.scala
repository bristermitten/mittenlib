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
import javax.lang.model.`type`.MirroredTypeException
import javax.lang.model.`type`.TypeMirror
import me.bristermitten.mittenlib.annotations.ast.CustomSerializerInfo
import me.bristermitten.mittenlib.config.SerializationContext
import me.bristermitten.mittenlib.config.extension.CustomSerializer
import me.bristermitten.mittenlib.config.extension.CustomSerializerFor
import me.bristermitten.mittenlib.config.tree.DataTree
import scala.jdk.CollectionConverters.*

@Singleton
class CustomSerializers extends CustomInfoRegistry[CustomSerializerInfo]:

  def registerCustomSerializer(customSerializerType: TypeElement): Unit =
    val annotation =
      customSerializerType.getAnnotation(classOf[CustomSerializerFor])
    if (annotation == null) {
      MessagerUtils.error(
        customSerializerType,
        "CustomSerializer must be annotated with @CustomSerializerFor"
      )
      return
    }

    val serializerFor =
      try
        val _ = annotation.value()
        throw new IllegalStateException("Expected MirroredTypeException")
      catch case e: MirroredTypeException => e.getTypeMirror

    val implementsCustomSerializer = TypeElementWrapper
      .wrap(customSerializerType)
      .getAllInterfaces
      .asScala
      .exists(i =>
        i.getQualifiedName.equals(classOf[CustomSerializer[?]].getCanonicalName)
      )

    val serializeMethodOpt = TypeElementWrapper
      .wrap(customSerializerType)
      .getMethods()
      .asScala
      .find { m =>
        m.getSimpleName == "serialize" &&
        m.getParameters.size() == 2 &&
        TypeUtils.TypeComparison.isTypeEqual(
          m.getParameters.get(0).asType().unwrap(),
          serializerFor
        ) &&
        m.getParameters
          .get(1)
          .asType()
          .toString == classOf[SerializationContext].getCanonicalName
      }

    if (serializeMethodOpt.isDefined) {
      val method = serializeMethodOpt.get
      if (
        method.getReturnType
          .unwrap()
          .toString != classOf[DataTree].getCanonicalName
      ) {
        MessagerUtils.error(
          method.unwrap(),
          "Custom serializer method must return DataTree"
        )
        return
      }
    }

    if (!implementsCustomSerializer && serializeMethodOpt.isEmpty) {
      MessagerUtils.error(
        customSerializerType,
        "CustomSerializer must implement CustomSerializer or have a static method DataTree serialize(T, SerializationContext)"
      )
      return
    }

    val isStatic = !implementsCustomSerializer &&
      serializeMethodOpt.isDefined &&
      serializeMethodOpt.get.unwrap().getModifiers.contains(Modifier.STATIC)

    if (!isStatic && !implementsCustomSerializer) {
      MessagerUtils.error(
        customSerializerType,
        "Non static custom serializers must implement CustomSerializer"
      )
      return
    }

    val customSerializerInfo =
      CustomSerializerInfo(customSerializerType, isStatic)

    register(TypeName.get(serializerFor), customSerializerInfo)
