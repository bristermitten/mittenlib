package me.bristermitten.mittenlib.annotations.util

import javax.lang.model.element.Element
import javax.lang.model.element.VariableElement

class Stringify private ()

/** Generic utilities for converting objects to strings. */
object Stringify:

  /** Returns a generic pretty string representation of the given
    * {@link Element} object.
    *
    * For a VariableElement, the format is "{type} {name} in class
    * {enclosingClass}",
    *
    * @param element
    *   the Element object to generate the string representation of
    * @return
    *   a string representation of the given Element object
    */
  def prettyStringify(element: Element): String =
    element match
      case variableElement: VariableElement =>
        s"${variableElement.asType()} ${variableElement.getSimpleName.toString} in class ${prettyStringify(variableElement.getEnclosingElement)}"
      case _ =>
        element.toString
