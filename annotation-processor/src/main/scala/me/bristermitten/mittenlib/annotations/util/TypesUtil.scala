package me.bristermitten.mittenlib.annotations.util

import com.google.inject.Inject
import com.palantir.javapoet.ClassName
import com.palantir.javapoet.ParameterizedTypeName
import com.palantir.javapoet.TypeName
import java.lang.annotation.Annotation
import javax.lang.model.AnnotatedConstruct
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.`type`.DeclaredType
import javax.lang.model.`type`.PrimitiveType
import javax.lang.model.`type`.TypeKind
import javax.lang.model.`type`.TypeMirror
import javax.lang.model.`type`.ExecutableType
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import me.bristermitten.mittenlib.annotations.compile.GeneratedTypeCache
import me.bristermitten.mittenlib.annotations.exception.DTOReferenceException
import me.bristermitten.mittenlib.config.Config
import me.bristermitten.mittenlib.config.generate.CascadeToInnerClasses
import me.bristermitten.mittenlib.config.tree.DataTree
import org.jspecify.annotations.Nullable
import scala.jdk.CollectionConverters.*

/** Helper class for working with {@link TypeMirror}s */
class TypesUtil @Inject() (
    private val types: Types,
    private val elements: Elements,
    private val generatedTypeCache: GeneratedTypeCache
):

  /** Get a "safe" version of a type, where "safe" refers to being able to use
    * it as the target of a <code>instanceof</code> check without compilation
    * errors This is defined as the boxed type for primitives, the erasure for
    * parameterized types, otherwise simply the type itself Examples:
    *
    * <ul> <li>{@code int -> Integer} <li>{@code Map<String, Integer> -> Map}
    * <li>{@code String -> String} </ul>
    */
  def getSafeType(typeMirror: TypeMirror): TypeMirror =
    if (typeMirror.getKind.isPrimitive) {
      types.boxedClass(typeMirror.asInstanceOf[PrimitiveType]).asType()
    } else {
      types.erasure(typeMirror)
    }

  /** Get a boxed version of a given type, if it is a primitive. Otherwise, the
    * type is returned unchanged
    */
  def getBoxedType(typeMirror: TypeMirror): TypeMirror =
    if (typeMirror.getKind.isPrimitive) {
      types.boxedClass(typeMirror.asInstanceOf[PrimitiveType]).asType()
    } else {
      typeMirror
    }

  private def hasNullableAnnotation(construct: AnnotatedConstruct): Boolean =
    construct.getAnnotationMirrors.asScala.exists(ann =>
      ann.getAnnotationType.asElement().getSimpleName.toString == "Nullable"
    )

  /** Return if a {@link VariableElement} should be considered nullable or not
    * Everything is considered non-nullable unless it is specifically annotated
    * as nullable. Any annotation named "Nullable" is supported, i.e. jetbrains
    * or javax
    *
    * @param element
    *   The element to check
    * @return
    *   True if the element is nullable, false otherwise
    */
  def isNullable(element: Element): Boolean =
    val baseNullable = element match
      case variableElement: VariableElement =>
        isNullable(variableElement.asType())
      case exec: ExecutableElement =>
        isNullable(exec.getReturnType)
      case _ =>
        false

    baseNullable || hasNullableAnnotation(element)

  def isNullable(typeMirror: TypeMirror): Boolean =
    !typeMirror.getKind.isPrimitive && hasNullableAnnotation(typeMirror)

  /** Gets an {@link Annotation} present on an {@link Element}, if present. This
    * method is slightly different to {@link Element#getAnnotation(Class)}, in
    * that it respects the semantics described in {@link CascadeToInnerClasses}
    *
    * @param e
    *   The element
    * @param annotationType
    *   The class of the annotation
    * @tparam A
    *   The annotation type
    * @return
    *   The annotation value, if present, else null
    */
  def getAnnotation[A <: Annotation](e: Element, annotationType: Class[A]): A =
    val onElem = e.getAnnotation(annotationType)
    if (onElem != null) {
      onElem
    } else if (
      annotationType.getAnnotation(classOf[CascadeToInnerClasses]) != null
    ) {
      val enclosing = e.getEnclosingElement
      if (enclosing == null) {
        null.asInstanceOf[A]
      } else {
        getAnnotation(enclosing, annotationType)
      }
    } else {
      null.asInstanceOf[A]
    }

  /** Checks if a type is a config type (annotated with @Config).
    *
    * @param mirror
    *   The type to check
    * @return
    *   true if the type is a config type, false otherwise
    */
  def isConfigType(mirror: TypeMirror): Boolean =
    if (mirror.getKind == TypeKind.ERROR) {
      throw DTOReferenceException(mirror, generatedTypeCache, null, null)
    }
    mirror match
      case declaredType: DeclaredType =>
        getAnnotation(declaredType.asElement(), classOf[Config]) != null
      case _ =>
        false

  /** Checks if a type is a newtype (annotated with @Newtype).
    *
    * @param mirror
    *   The type to check
    * @return
    *   true if the type is a newtype, false otherwise
    */
  def isNewtype(mirror: TypeMirror): Boolean =
    mirror match
      case declaredType: DeclaredType =>
        NewtypeUtil.isNewtype(declaredType.asElement())
      case _ =>
        false

  def getNewtypeUnderlyingType(newtypeMirror: TypeMirror): TypeMirror =
    newtypeMirror match
      case declaredType: DeclaredType =>
        val element = declaredType.asElement().asInstanceOf[TypeElement]
        if (element.getKind == javax.lang.model.element.ElementKind.RECORD) {
          val component = NewtypeUtil.getNewtypeRecordComponent(element)
          val accessor = component.getAccessor
          val resolvedMethod = types
            .asMemberOf(declaredType, accessor)
            .asInstanceOf[ExecutableType]
          resolvedMethod.getReturnType
        } else if (
          element.getKind == javax.lang.model.element.ElementKind.INTERFACE
        ) {
          val method = NewtypeUtil.getAbstractMethod(element)
          val resolvedMethod =
            types.asMemberOf(declaredType, method).asInstanceOf[ExecutableType]
          resolvedMethod.getReturnType
        } else {
          throw new IllegalArgumentException(
            s"Newtype annotation only supports records and interfaces: $element"
          )
        }
      case _ =>
        throw new IllegalArgumentException(
          s"Newtype must be a DeclaredType: $newtypeMirror"
        )

  def getDataTreeType(tpe: TypeName): Option[TypeName] =
    val unboxedType = if (tpe.isBoxedPrimitive) tpe.unbox() else tpe
    if (
      unboxedType.equals(TypeName.INT)
      || unboxedType.equals(TypeName.LONG)
      || unboxedType.equals(TypeName.SHORT)
      || unboxedType.equals(TypeName.BYTE)
    ) {
      Some(ClassName.get(classOf[DataTree.DataTreeLiteral.DataTreeLiteralInt]))
    } else if (
      unboxedType.equals(TypeName.FLOAT) || unboxedType.equals(TypeName.DOUBLE)
    ) {
      Some(
        ClassName.get(classOf[DataTree.DataTreeLiteral.DataTreeLiteralFloat])
      )
    } else if (unboxedType.equals(TypeName.BOOLEAN)) {
      Some(
        ClassName.get(classOf[DataTree.DataTreeLiteral.DataTreeLiteralBoolean])
      )
    } else if (unboxedType.equals(ClassName.get(classOf[String]))) {
      Some(
        ClassName.get(classOf[DataTree.DataTreeLiteral.DataTreeLiteralString])
      )
    } else {
      unboxedType match
        case p: ParameterizedTypeName =>
          if (p.rawType().equals(ClassName.get(classOf[java.util.Map[?, ?]]))) {
            Some(ClassName.get(classOf[DataTree.DataTreeMap]))
          } else if (
            p.rawType().equals(ClassName.get(classOf[java.util.List[?]]))
          ) {
            Some(ClassName.get(classOf[DataTree.DataTreeArray]))
          } else {
            None
          }
        case _ =>
          None
    }

  def isList(tpe: TypeMirror): Boolean =
    isSubtypeOf(tpe, "java.util.List")

  def isSet(tpe: TypeMirror): Boolean =
    isSubtypeOf(tpe, "java.util.Set")

  def isMap(tpe: TypeMirror): Boolean =
    isSubtypeOf(tpe, "java.util.Map")

  def isCollection(tpe: TypeMirror): Boolean =
    isSubtypeOf(tpe, "java.util.Collection")

  private def isSubtypeOf(tpe: TypeMirror, qualifiedName: String): Boolean =
    tpe match
      case declaredType: DeclaredType =>
        val element = declaredType.asElement()
        element match
          case typeElement: TypeElement =>
            val targetElement = elements.getTypeElement(qualifiedName)
            if (targetElement == null) {
              false
            } else {
              val targetErasure = types.erasure(targetElement.asType())
              val typeErasure = types.erasure(tpe)
              types.isAssignable(typeErasure, targetErasure)
            }
          case _ =>
            false
      case _ =>
        false

object TypesUtil:
  /** Get the {@link TypeMirror} representing the type of an {@link Element}.
    * Supports {@link VariableElement} and {@link ExecutableElement}.
    *
    * @param element
    *   the element to get the type of
    * @return
    *   the type mirror representing the element's type
    */
  def getType(element: Element): TypeMirror =
    element match
      case variableElement: VariableElement =>
        variableElement.asType()
      case executableElement: ExecutableElement =>
        executableElement.getReturnType
      case _ =>
        throw new IllegalStateException(s"Unexpected kind: ${element.getKind}")
