package me.bristermitten.mittenlib.annotations.util

import com.google.inject.Inject
import io.toolisticon.aptk.tools.TypeMirrorWrapper
import me.bristermitten.mittenlib.config.ConfigTransient

import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.util.Elements
import scala.jdk.CollectionConverters.*

/** Helper class for working with {@link Elements} */
class ElementsFinder @Inject() (private val elements: Elements):

  /** Get all the {@link VariableElement}s in a given {@link TypeElement} that
    * should be included in the generated config class.
    *
    * This does not include fields with the <code>transient</code> or
    * <code>static</code> modifiers, or fields inherited from superclasses.
    *
    * @param rootElement
    *   The element to find variables in
    * @return
    *   All the {@link VariableElement}s in the given element that are suitable
    *   for config generation
    */
  def getApplicableVariableElements(
      rootElement: TypeElement
  ): List[VariableElement] =
    elements
      .getAllMembers(rootElement)
      .asScala
      .filter(_.getEnclosingElement == rootElement)
      .filter(_.getKind.isField)
      .map(_.asInstanceOf[VariableElement])
      .filter(!_.getModifiers.contains(Modifier.TRANSIENT))
      .filter(!_.getModifiers.contains(Modifier.STATIC))
      .toList

  /** Get all the methods in a given {@link TypeElement}, including inherited
    * ones. This will only return methods, not constructors or initializers.
    *
    * @param rootElement
    *   The type to search for methods
    * @return
    *   All the methods in the given type
    */
  def getAllMethods(
      rootElement: TypeElement
  ): List[ExecutableElement] =
    elements
      .getAllMembers(rootElement)
      .asScala
      .filter(_.getKind == ElementKind.METHOD)
      .map(_.asInstanceOf[ExecutableElement])
      .toList

  def getPropertyMethods(
      rootElement: TypeElement
  ): List[ExecutableElement] =
    getAllMethods(rootElement)
      .filter(_.getParameters.isEmpty)
      .filter(method =>
        method.getAnnotation(classOf[ConfigTransient]) == null
      ) // ignore transient
      .filter(method =>
        TypeMirrorWrapper
          .wrap(method.getEnclosingElement.asType())
          .getQualifiedName != "java.lang.Object"
      )
