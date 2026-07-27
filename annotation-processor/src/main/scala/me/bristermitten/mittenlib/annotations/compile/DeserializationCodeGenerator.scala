package me.bristermitten.mittenlib.annotations.compile

import com.google.inject.Inject
import com.palantir.javapoet.*
import io.toolisticon.aptk.tools.TypeMirrorWrapper
import io.toolisticon.aptk.tools.wrapper.TypeElementWrapper
import java.util.Optional
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.`type`.TypeMirror
import me.bristermitten.mittenlib.annotations.domain.{ConfigStructure, Property}
import me.bristermitten.mittenlib.annotations.compile.deserializer.{
  GenericTypeDeserializerGenerator,
  NonGenericTypeDeserializerGenerator
}
import me.bristermitten.mittenlib.annotations.util.TypesUtil
import me.bristermitten.mittenlib.config.DeserializationContext
import me.bristermitten.mittenlib.config.exception.ConfigLoadingErrors
import me.bristermitten.mittenlib.config.tree.DataTree
import me.bristermitten.mittenlib.util.Result
import org.jspecify.annotations.Nullable

import _root_.me.bristermitten.mittenlib.codegen.dsl.*
import _root_.me.bristermitten.mittenlib.codegen.dsl.BlockBuilder.*
import _root_.me.bristermitten.mittenlib.codegen.dsl.given

class DeserializationCodeGenerator @Inject() (
    val typesUtil: TypesUtil,
    private val fieldNameGenerator: FieldNameGenerator,
    private val methodNames: MethodNames,
    private val configurationClassNameGenerator: ConfigurationClassNameGenerator,
    private val genericTypeDeserializerGenerator: GenericTypeDeserializerGenerator,
    private val nonGenericTypeDeserializerGenerator: NonGenericTypeDeserializerGenerator
):

  def createDeserializeMethodFor(
      dtoClassName: ClassName,
      propertyAST: ConfigStructure,
      property: Property,
      @Nullable daoName: ClassName
  ): MethodSpec =
    val elementType = property.typeMirror
    val elementResultType =
      configurationClassNameGenerator.publicPropertyClassName(
        typesUtil.getBoxedType(property.typeMirror)
      )

    val wrappedElementType = TypeMirrorWrapper.wrap(elementType)
    val isGenericType =
      wrappedElementType.hasTypeArguments && !typesUtil.isNewtype(elementType)
    val typeElementOpt = wrappedElementType.getTypeElement

    if (isGenericType && typeElementOpt.isPresent) {
      genericTypeDeserializerGenerator.generateDeserializeMethod(
        propertyAST,
        property,
        dtoClassName,
        elementType,
        wrappedElementType,
        typeElementOpt.get(),
        elementResultType,
        daoName,
        fieldNameGenerator,
        methodNames
      )
    } else {
      nonGenericTypeDeserializerGenerator.generateDeserializeMethod(
        propertyAST,
        property,
        dtoClassName,
        elementType,
        wrappedElementType,
        elementResultType,
        daoName,
        fieldNameGenerator,
        methodNames
      )
    }
