package me.bristermitten.mittenlib.annotations.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import io.toolisticon.aptk.common.ToolingProvider;
import io.toolisticon.cute.Cute;
import io.toolisticon.cute.PassIn;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import me.bristermitten.mittenlib.annotations.compile.GeneratedTypeCache;
import me.bristermitten.mittenlib.annotations.exception.DTOReferenceException;
import me.bristermitten.mittenlib.config.Config;
import me.bristermitten.mittenlib.config.generate.CascadeToInnerClasses;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

class TypesUtilTest {

    @Retention(RetentionPolicy.RUNTIME)
    @CascadeToInnerClasses
    @interface CascadingAnnotation {}

    @Config
    @CascadingAnnotation
    @PassIn
    static class TestConfigClass {
        @Nullable String nullableField;

        int nonNullableField;

        @Nullable String nullableMethod() {
            return null;
        }

        String nonNullableMethod() {
            return "";
        }

        @Config
        static class InnerClass {}
    }

    static class NonConfigClass {}

    @Test
    void testTypesUtil() {
        Cute.unitTest()
                .when()
                .passInElement()
                .<TypeElement>fromClass(TestConfigClass.class)
                .intoUnitTest((processingEnvironment, element) -> {
                    ToolingProvider.setTooling(processingEnvironment);
                    TypesUtil typesUtil = new TypesUtil(
                            processingEnvironment.getTypeUtils(),
                            processingEnvironment.getElementUtils(),
                            new GeneratedTypeCache());

                    // 1. getSafeType and getBoxedType
                    TypeMirror intType = processingEnvironment.getTypeUtils().getPrimitiveType(TypeKind.INT);
                    assertThat(typesUtil.getSafeType(intType).toString()).isEqualTo("java.lang.Integer");
                    assertThat(typesUtil.getBoxedType(intType).toString()).isEqualTo("java.lang.Integer");

                    TypeMirror stringType = processingEnvironment
                            .getElementUtils()
                            .getTypeElement("java.lang.String")
                            .asType();
                    assertThat(typesUtil.getSafeType(stringType)).isEqualTo(stringType);
                    assertThat(typesUtil.getBoxedType(stringType)).isEqualTo(stringType);

                    // 2. Nullability checks on Element
                    VariableElement nullableField = null;
                    VariableElement nonNullableField = null;
                    ExecutableElement nullableMethod = null;
                    ExecutableElement nonNullableMethod = null;

                    for (var enclosed : element.getEnclosedElements()) {
                        if (enclosed instanceof VariableElement var) {
                            if (var.getSimpleName().toString().equals("nullableField")) {
                                nullableField = var;
                            } else if (var.getSimpleName().toString().equals("nonNullableField")) {
                                nonNullableField = var;
                            }
                        } else if (enclosed instanceof ExecutableElement exec) {
                            if (exec.getSimpleName().toString().equals("nullableMethod")) {
                                nullableMethod = exec;
                            } else if (exec.getSimpleName().toString().equals("nonNullableMethod")) {
                                nonNullableMethod = exec;
                            }
                        }
                    }

                    assertThat(nullableField).isNotNull();
                    assertThat(nonNullableField).isNotNull();
                    assertThat(nullableMethod).isNotNull();
                    assertThat(nonNullableMethod).isNotNull();

                    // 3. Nullability checks on TypeMirror
                    assertThat(typesUtil.isNullable(intType)).isFalse();

                    // 4. getAnnotation and Cascading
                    assertThat(typesUtil.getAnnotation(element, CascadingAnnotation.class))
                            .isNotNull();

                    // Check inner class inherits CascadingAnnotation
                    TypeElement innerElement = null;
                    for (var enclosed : element.getEnclosedElements()) {
                        if (enclosed instanceof TypeElement type
                                && type.getSimpleName().toString().equals("InnerClass")) {
                            innerElement = type;
                        }
                    }
                    assertThat(innerElement).isNotNull();
                    assertThat(typesUtil.getAnnotation(innerElement, CascadingAnnotation.class))
                            .isNotNull();

                    // Enclosing element null case
                    TypeElement topLevelElement =
                            processingEnvironment.getElementUtils().getTypeElement("java.lang.String");
                    assertThat(typesUtil.getAnnotation(topLevelElement, CascadingAnnotation.class))
                            .isNull();

                    // 5. isConfigType
                    assertThat(typesUtil.isConfigType(element.asType())).isTrue();

                    TypeMirror nonConfigType = processingEnvironment
                            .getElementUtils()
                            .getTypeElement(NonConfigClass.class.getCanonicalName())
                            .asType();
                    assertThat(typesUtil.isConfigType(nonConfigType)).isFalse();

                    // non-DeclaredType ConfigType check
                    assertThat(typesUtil.isConfigType(intType)).isFalse();

                    // 6. getDataTreeType checks
                    assertThat(typesUtil.getDataTreeType(TypeName.INT)).isPresent();
                    assertThat(typesUtil.getDataTreeType(TypeName.LONG)).isPresent();
                    assertThat(typesUtil.getDataTreeType(TypeName.SHORT)).isPresent();
                    assertThat(typesUtil.getDataTreeType(TypeName.BYTE)).isPresent();
                    assertThat(typesUtil.getDataTreeType(TypeName.FLOAT)).isPresent();
                    assertThat(typesUtil.getDataTreeType(TypeName.DOUBLE)).isPresent();
                    assertThat(typesUtil.getDataTreeType(TypeName.BOOLEAN)).isPresent();
                    assertThat(typesUtil.getDataTreeType(ClassName.get(String.class)))
                            .isPresent();

                    TypeName mapTypeName = ParameterizedTypeName.get(Map.class, String.class, Integer.class);
                    assertThat(typesUtil.getDataTreeType(mapTypeName)).isPresent();

                    TypeName listTypeName = ParameterizedTypeName.get(List.class, String.class);
                    assertThat(typesUtil.getDataTreeType(listTypeName)).isPresent();
                    assertThat(typesUtil.getDataTreeType(TypeName.VOID)).isEmpty();

                    // 7. Subtype checks
                    TypeMirror listMirror = processingEnvironment
                            .getElementUtils()
                            .getTypeElement("java.util.List")
                            .asType();
                    TypeMirror arrayListMirror = processingEnvironment
                            .getElementUtils()
                            .getTypeElement("java.util.ArrayList")
                            .asType();
                    TypeMirror setMirror = processingEnvironment
                            .getElementUtils()
                            .getTypeElement("java.util.Set")
                            .asType();
                    TypeMirror mapMirror = processingEnvironment
                            .getElementUtils()
                            .getTypeElement("java.util.Map")
                            .asType();

                    assertThat(typesUtil.isList(listMirror)).isTrue();
                    assertThat(typesUtil.isList(arrayListMirror)).isTrue();
                    assertThat(typesUtil.isSet(setMirror)).isTrue();
                    assertThat(typesUtil.isMap(mapMirror)).isTrue();
                    assertThat(typesUtil.isCollection(listMirror)).isTrue();

                    // Edge cases in isSubtypeOf
                    assertThat(typesUtil.isList(intType)).isFalse(); // not DeclaredType
                })
                .thenExpectThat()
                .compilationSucceeds()
                .executeTest();
    }

    @Test
    void testConfigTypeExceptionOnErrorType() {
        Cute.unitTest()
                .when()
                .passInElement()
                .<TypeElement>fromClass(TestConfigClass.class)
                .intoUnitTest((processingEnvironment, element) -> {
                    ToolingProvider.setTooling(processingEnvironment);
                    TypesUtil typesUtil = new TypesUtil(
                            processingEnvironment.getTypeUtils(),
                            processingEnvironment.getElementUtils(),
                            new GeneratedTypeCache());

                    // Create a mocked TypeMirror returning TypeKind.ERROR
                    TypeMirror mockErrorMirror = mock(TypeMirror.class);
                    when(mockErrorMirror.getKind()).thenReturn(TypeKind.ERROR);

                    assertThatThrownBy(() -> typesUtil.isConfigType(mockErrorMirror))
                            .isInstanceOf(DTOReferenceException.class);
                })
                .thenExpectThat()
                .compilationSucceeds()
                .executeTest();
    }
}
