package me.bristermitten.mittenlib.annotations.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import javax.lang.model.element.Element;
import javax.lang.model.element.Name;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import org.junit.jupiter.api.Test;

class StringifyTest {

    @Test
    void testPrivateConstructor() throws Exception {
        Constructor<Stringify> constructor = Stringify.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        try {
            constructor.newInstance();
        } catch (InvocationTargetException e) {
            throw (Exception) e.getCause();
        }
    }

    @Test
    void testPrettyStringifyGenericElement() {
        Element element = mock(Element.class);
        when(element.toString()).thenReturn("SomeGenericElement");

        String result = Stringify.prettyStringify(element);
        assertEquals("SomeGenericElement", result);
    }

    @Test
    void testPrettyStringifyVariableElement() {
        VariableElement variableElement = mock(VariableElement.class);
        TypeMirror typeMirror = mock(TypeMirror.class);
        Name name = mock(Name.class);
        Element enclosingElement = mock(Element.class);

        when(typeMirror.toString()).thenReturn("int");
        when(name.toString()).thenReturn("myField");
        when(enclosingElement.toString()).thenReturn("MyClass");

        when(variableElement.asType()).thenReturn(typeMirror);
        when(variableElement.getSimpleName()).thenReturn(name);
        when(variableElement.getEnclosingElement()).thenReturn(enclosingElement);

        String result = Stringify.prettyStringify(variableElement);
        assertEquals("int myField in class MyClass", result);
    }
}
