package me.bristermitten.mittenlib.annotations.parser;

import io.toolisticon.aptk.common.ToolingProvider;
import io.toolisticon.cute.Cute;
import javax.lang.model.element.TypeElement;
import org.junit.jupiter.api.Test;

class CustomSerializersTest {

    @Test
    void testRegisterCustomSerializerMissingAnnotationReportsError() {
        Cute.unitTest()
                .when()
                .passInElement()
                .<TypeElement>fromSourceString(
                        "me.bristermitten.mittenlib.annotations.parser.MissingAnnotationSerializer", """
                        package me.bristermitten.mittenlib.annotations.parser;
                        import io.toolisticon.cute.PassIn;
                        @PassIn
                        class MissingAnnotationSerializer {}
                        """)
                .intoUnitTest((processingEnvironment, element) -> {
                    ToolingProvider.setTooling(processingEnvironment);
                    CustomSerializers registry = new CustomSerializers();

                    registry.registerCustomSerializer(element);
                })
                .thenExpectThat()
                .compilationFails()
                .executeTest();
    }

    @Test
    void testRegisterCustomSerializerInvalidInterfaceOrMethodReportsError() {
        Cute.unitTest()
                .when()
                .passInElement()
                .<TypeElement>fromSourceString("me.bristermitten.mittenlib.annotations.parser.InvalidSerializer", """
                        package me.bristermitten.mittenlib.annotations.parser;
                        import io.toolisticon.cute.PassIn;
                        import me.bristermitten.mittenlib.config.extension.CustomSerializerFor;
                        @CustomSerializerFor(String.class)
                        @PassIn
                        class InvalidSerializer {}
                        """)
                .intoUnitTest((processingEnvironment, element) -> {
                    ToolingProvider.setTooling(processingEnvironment);
                    CustomSerializers registry = new CustomSerializers();

                    registry.registerCustomSerializer(element);
                })
                .thenExpectThat()
                .compilationFails()
                .executeTest();
    }
}
