package me.bristermitten.mittenlib.annotations.parser;

import static org.assertj.core.api.Assertions.assertThat;

import io.toolisticon.aptk.common.ToolingProvider;
import io.toolisticon.cute.Cute;
import io.toolisticon.cute.PassIn;
import java.util.List;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import me.bristermitten.mittenlib.annotations.util.ElementsFinder;
import org.junit.jupiter.api.Test;

class ElementsFinderTest {

    @Test
    void testGetApplicableVariableElements() {
        Cute.unitTest()
                .when()
                .passInElement()
                .<TypeElement>fromClass(TestClass.class)
                .intoUnitTest((processingEnvironment, element) -> {
                    ToolingProvider.setTooling(processingEnvironment);
                    ElementsFinder finder = new ElementsFinder(processingEnvironment.getElementUtils());

                    List<VariableElement> fields = finder.getApplicableVariableElements(element);
                    assertThat(fields)
                            .extracting(f -> f.getSimpleName().toString())
                            .containsExactlyInAnyOrder(
                                    "publicField", "protectedField", "packagePrivateField", "privateField");
                    assertThat(fields)
                            .extracting(f -> f.getSimpleName().toString())
                            .doesNotContain("staticField", "transientField");
                })
                .thenExpectThat()
                .compilationSucceeds()
                .executeTest();
    }

    @Test
    void testGetPropertyMethods() {
        Cute.unitTest()
                .when()
                .passInElement()
                .<TypeElement>fromClass(TestClass.class)
                .intoUnitTest((processingEnvironment, element) -> {
                    ToolingProvider.setTooling(processingEnvironment);
                    ElementsFinder finder = new ElementsFinder(processingEnvironment.getElementUtils());

                    List<ExecutableElement> methods = finder.getPropertyMethods(element);
                    assertThat(methods)
                            .extracting(m -> m.getSimpleName().toString())
                            .contains("getPublicMethod", "privateMethod", "staticMethod", "voidMethod");
                    assertThat(methods)
                            .extracting(m -> m.getSimpleName().toString())
                            .doesNotContain("methodWithParams");
                })
                .thenExpectThat()
                .compilationSucceeds()
                .executeTest();
    }

    @PassIn
    @SuppressWarnings("unused")
    static class TestClass {
        public int publicField;
        protected int protectedField;
        int packagePrivateField;
        private int privateField;
        public static int staticField;
        public transient int transientField;

        public int getPublicMethod() {
            return 0;
        }

        private int privateMethod() {
            return 0;
        }

        public static int staticMethod() {
            return 0;
        }

        public void voidMethod() {}

        public int methodWithParams(int x) {
            return x;
        }
    }
}
