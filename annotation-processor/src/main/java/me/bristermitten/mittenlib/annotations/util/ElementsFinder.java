package me.bristermitten.mittenlib.annotations.util;

import io.toolisticon.aptk.tools.TypeMirrorWrapper;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import javax.lang.model.element.*;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.List;


/**
 * Helper class for working with {@link Elements}
 */
public class ElementsFinder {

    private final Elements elements;

    private final Types types;

    @Inject
    ElementsFinder(Elements elements, Types types) {
        this.elements = elements;
        this.types = types;
    }

    /**
     * Get all the {@link VariableElement}s in a given {@link TypeElement}
     * that should be included in the generated config class.
     * <p>
     * This does not include fields with the <code>transient</code> or <code>static</code>
     * modifiers, or fields inherited from superclasses.
     *
     * @param rootElement The element to find variables in
     * @return All the {@link VariableElement}s in the given element that are suitable for config generation
     */
    public @NotNull List<VariableElement> getApplicableVariableElements(TypeElement rootElement) {
        return elements.getAllMembers(rootElement).stream()
                .filter(elem -> elem.getEnclosingElement().equals(rootElement)) // elements#getAllMembers seems quite unpredictable as to whether it returns members from the superclass, so we'll just remove them in case
                .filter(element -> element.getKind().isField())
                .map(VariableElement.class::cast)
                .filter(elem -> !elem.getModifiers().contains(Modifier.TRANSIENT)) // ignore transient fields
                .filter(elem -> !elem.getModifiers().contains(Modifier.STATIC)) // ignore static fields
                .toList();
    }

    /**
     * Get all the methods in a given {@link TypeElement}, including inherited ones.
     * This will only return methods, not constructors or initializers.
     *
     * @param rootElement The type to search for methods
     * @return All the methods in the given type
     */
    public @NotNull List<ExecutableElement> getAllMethods(TypeElement rootElement) {
        return elements.getAllMembers(rootElement).stream()
                .filter(element -> element.getKind() == ElementKind.METHOD)
                .map(ExecutableElement.class::cast)
                .toList();
    }


    public @NotNull List<ExecutableElement> getPropertyMethods(TypeElement rootElement) {
        return elements.getAllMembers(rootElement).stream()
                .filter(element -> element.getKind() == ElementKind.METHOD)
                .map(ExecutableElement.class::cast)
                .filter(method -> method.getParameters().isEmpty()) // Only getters
                // remove java.util.Object methods
                .filter(method -> !TypeMirrorWrapper.wrap
                        (method.getEnclosingElement().asType()).getQualifiedName().equals("java.lang.Object"))
                .toList();
    }

}
