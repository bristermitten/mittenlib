package me.bristermitten.mittenlib.annotations.config

import com.google.inject.Singleton
import me.bristermitten.mittenlib.annotations.util.ElementsFinder
import javax.inject.Inject
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement

@Singleton
class MethodNames @Inject internal constructor(private val elementsFinder: ElementsFinder) {
    private val safeNameCache: MutableMap<VariableElement, String> = HashMap()
    private val safeNameCache2: MutableMap<VariableElement, Set<String>> = HashMap()
    fun safeMethodName(variableElement: VariableElement, enclosingClass: TypeElement): String {
        return safeNameCache.computeIfAbsent(
            variableElement
        ) { elem: VariableElement -> safeMethodName0(elem, enclosingClass) }
    }

    private fun safeMethodName0(variableElement: VariableElement, enclosingClass: TypeElement): String {
        val methodNames =
            safeNameCache2.getOrPut(variableElement) { getNoArgMethodNames(enclosingClass) }
        val name = StringBuilder(variableElement.simpleName)
        while (methodNames.contains(name.toString())) {
            name.append("_")
        }
        return name.toString()
    }

    private fun getNoArgMethodNames(enclosingClass: TypeElement): Set<String> {
        val names = HashSet<String>()
        for (method in elementsFinder.getAllMethods(enclosingClass)) {
            if (!method.parameters.isEmpty()) {
                continue
            }
            names.add(method.simpleName.toString())
        }
        return names
    }
}
