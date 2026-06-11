package me.bristermitten.mittenlib.annotations.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.palantir.javapoet.AnnotationSpec;
import me.bristermitten.mittenlib.annotations.ast.ASTSettings;
import me.bristermitten.mittenlib.annotations.ast.Property;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

class NullityTest {

    @Test
    void testConstructor() {
        Nullity nullity = new Nullity();
        assertNotNull(nullity);
    }

    @Test
    void testGetNullityAnnotationNullable() {
        Property property = mock(Property.class);
        ASTSettings.PropertyASTSettings settings = mock(ASTSettings.PropertyASTSettings.class);

        when(property.settings()).thenReturn(settings);
        when(settings.isNullable()).thenReturn(true);

        assertEquals(Nullable.class, Nullity.getNullityAnnotation(property));

        AnnotationSpec spec = Nullity.getNullityAnnotationSpec(property);
        assertEquals(Nullable.class.getName(), spec.type().toString());
    }

    @Test
    void testGetNullityAnnotationNonNull() {
        Property property = mock(Property.class);
        ASTSettings.PropertyASTSettings settings = mock(ASTSettings.PropertyASTSettings.class);

        when(property.settings()).thenReturn(settings);
        when(settings.isNullable()).thenReturn(false);

        assertEquals(NonNull.class, Nullity.getNullityAnnotation(property));

        AnnotationSpec spec = Nullity.getNullityAnnotationSpec(property);
        assertEquals(NonNull.class.getName(), spec.type().toString());
    }
}
