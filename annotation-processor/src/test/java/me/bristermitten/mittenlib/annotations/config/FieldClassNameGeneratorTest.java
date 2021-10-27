package me.bristermitten.mittenlib.annotations.config;

import me.bristermitten.mittenlib.config.names.ConfigName;
import me.bristermitten.mittenlib.config.names.NamingPattern;
import me.bristermitten.mittenlib.config.names.NamingPatterns;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.lang.model.element.Element;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static me.bristermitten.mittenlib.annotations.config.FieldClassNameGenerator.getConfigFieldName;
import static me.bristermitten.mittenlib.config.names.NamingPatternTransformer.format;
import static me.bristermitten.mittenlib.config.names.NamingPatterns.LOWER_SNAKE_CASE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class FieldClassNameGeneratorTest {

    static TypeElement createClassElementMock(Name name, Collection<Annotation> annotations) {
        final TypeElement mock = Mockito.mock(TypeElement.class);
        when(mock.getSimpleName()).thenReturn(name);
        when(mock.getAnnotation(any())).then(invocation -> {
            final Class<? extends Annotation> arg = invocation.getArgument(0);
            for (Annotation annotation : annotations) {
                if (arg.isInstance(annotation)) {
                    return annotation;
                }
            }
            return null;
        });
        return mock;
    }

    static VariableElement createVariableElementMock(Name name, Collection<Annotation> annotations, Element enclosingClass) {
        final VariableElement mock = Mockito.mock(VariableElement.class);
        when(mock.getSimpleName()).thenReturn(name);
        when(mock.getConstantValue()).thenReturn(null);
        when(mock.getAnnotation(any())).then(invocation -> {
            final Class<? extends Annotation> arg = invocation.getArgument(0);
            for (Annotation annotation : annotations) {
                if (arg.isInstance(annotation)) {
                    return annotation;
                }
            }
            return null;
        });
        when(mock.getEnclosingElement()).thenReturn(enclosingClass);
        return mock;
    }

    static Name mockName(String value) {
        return Mockito.mock(Name.class,
                invocation -> invocation.getMethod().invoke(value, invocation.getArguments()));
    }

    static ConfigName createConfigName(String name) {
        return new ConfigName() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return ConfigName.class;
            }

            @Override
            public String value() {
                return name;
            }
        };
    }

    static NamingPattern createNamingPattern(NamingPatterns pattern) {
        return new NamingPattern() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return NamingPattern.class;
            }

            @Override
            public NamingPatterns value() {
                return pattern;
            }
        };
    }


    @Test
    void assertThat_unannotatedFieldName_isIdentity() {
        assertEquals("hello", getConfigFieldName(
                createVariableElementMock(mockName("hello"), Collections.emptyList(),
                        createClassElementMock(mockName("ClassName"), Collections.emptyList()))));
    }


    @Test
    void assertThat_annotatedFieldName_hasHigherPriority_withConfigName() {
        assertEquals("field-name", getConfigFieldName(
                createVariableElementMock(
                        mockName("hello"),
                        singletonList(createConfigName("field-name")),
                        createClassElementMock(mockName("ClassName"), Collections.emptyList()))));
    }

    @Test
    void assertThat_annotatedFieldName_hasHigherPriority_withNamingPattern() {
        assertEquals(format("fieldName", LOWER_SNAKE_CASE),
                getConfigFieldName(
                        createVariableElementMock(
                                mockName("fieldName"),
                                emptyList(),
                                createClassElementMock(mockName("ClassName"), singletonList(createNamingPattern(LOWER_SNAKE_CASE))))));
    }


    @Test
    void assertThat_unannotatedFieldName_usesClass_withNamingPattern() {
        assertEquals(format("fieldName", LOWER_SNAKE_CASE),
                getConfigFieldName(
                        createVariableElementMock(
                                mockName("fieldName"),
                                singletonList(createNamingPattern(LOWER_SNAKE_CASE)),
                                createClassElementMock(mockName("ClassName"), Collections.emptyList()))));
    }

}
