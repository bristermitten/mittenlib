package me.bristermitten.mittenlib.annotations.parser;

import io.toolisticon.aptk.common.ToolingProvider;
import io.toolisticon.cute.Cute;
import javax.lang.model.element.TypeElement;
import org.junit.jupiter.api.Test;

class CustomDeserializersTest {

    @Test
    void testRegisterCustomDeserializerMissingAnnotationReportsError() {
        Cute.unitTest()
                .when()
                .passInElement()
                .<TypeElement>fromSourceString(
                        "me.bristermitten.mittenlib.annotations.parser.MissingAnnotationDeserializer", """
                        package me.bristermitten.mittenlib.annotations.parser;
                        import io.toolisticon.cute.PassIn;
                        @PassIn
                        class MissingAnnotationDeserializer {}
                        """)
                .intoUnitTest((processingEnvironment, element) -> {
                    ToolingProvider.setTooling(processingEnvironment);
                    CustomDeserializers registry = new CustomDeserializers();

                    registry.registerCustomDeserializer(element);
                })
                .thenExpectThat()
                .compilationFails()
                .executeTest();
    }

    @Test
    void testRegisterCustomDeserializerInvalidInterfaceOrMethodReportsError() {
        Cute.unitTest()
                .when()
                .passInElement()
                .<TypeElement>fromSourceString("me.bristermitten.mittenlib.annotations.parser.InvalidDeserializer", """
                        package me.bristermitten.mittenlib.annotations.parser;
                        import io.toolisticon.cute.PassIn;
                        import me.bristermitten.mittenlib.config.extension.CustomDeserializerFor;
                        @CustomDeserializerFor(String.class)
                        @PassIn
                        class InvalidDeserializer {}
                        """)
                .intoUnitTest((processingEnvironment, element) -> {
                    ToolingProvider.setTooling(processingEnvironment);
                    CustomDeserializers registry = new CustomDeserializers();

                    registry.registerCustomDeserializer(element);
                })
                .thenExpectThat()
                .compilationFails()
                .executeTest();
    }
}
