package me.bristermitten.mittenlib.annotations.util

import javax.inject.Inject
import javax.lang.model.element.*
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

/**
 * Helper class for working with [Elements]
 */
class ElementsFinder @Inject internal constructor(private val elements: Elements, private val types: Types) {
    /**
     * Get all the [VariableElement]s in a given [TypeElement]
     * that should be included in the generated config class.
     *
     *
     * This does not include fields with the `transient` or `static`
     * modifiers, or fields inherited from superclasses.
     *
     * @param rootElement The element to find variables in
     * @return All the [VariableElement]s in the given element that are suitable for config generation
     */
    fun getApplicableVariableElements(rootElement: TypeElement): List<VariableElement> {
        return elements.getAllMembers(rootElement).stream()
            .filter { elem: Element -> elem.enclosingElement == rootElement } // elements#getAllMembers seems quite unpredictable as to whether it returns members from the superclass, so we'll just remove them in case
            .filter { element: Element -> element.kind.isField }
            .map { obj: Any? -> VariableElement::class.java.cast(obj) }
            .filter { elem: VariableElement -> !elem.modifiers.contains(Modifier.TRANSIENT) } // ignore transient fields
            .filter { elem: VariableElement -> !elem.modifiers.contains(Modifier.STATIC) } // ignore static fields
            .toList()
    }

    /**
     * @param rootElement The element to find variables in
     * @return All the [VariableElement]s in the given element that are suitable for config generation
     * @see .getApplicableVariableElements
     */
    fun getApplicableVariableElements(rootElement: TypeMirror?): List<VariableElement> {
        val elem = types.asElement(rootElement) as TypeElement
        return getApplicableVariableElements(elem)
    }

    /**
     * Get all the methods in a given [TypeElement], including inherited ones.
     * This will only return methods, not constructors or initializers.
     *
     * @param rootElement The type to search for methods
     * @return All the methods in the given type
     */
    fun getAllMethods(rootElement: TypeElement?): List<ExecutableElement> {
        return elements.getAllMembers(rootElement).stream()
            .filter { element: Element -> element.kind == ElementKind.METHOD }
            .map { obj: Any? -> ExecutableElement::class.java.cast(obj) }
            .toList()
    }
}
