package me.bristermitten.mittenlib.annotations.extension;

import io.toolisticon.aptk.common.ToolingProvider;
import io.toolisticon.cute.Cute;
import me.bristermitten.mittenlib.annotations.parser.CustomDeserializers;
import org.junit.jupiter.api.Test;

import javax.lang.model.element.TypeElement;

public class CustomDeserializerTest {
    @Test
    void test() {
        Cute.unitTest()
                .when()
                .passInElement()
                .<TypeElement>fromSourceString("me.bristermitten.mittenlib.annotations.integration.extension.CustomTypeDeserializer", """
                        import io.toolisticon.cute.PassIn;import me.bristermitten.mittenlib.annotations.integration.extension.CustomType;
                        import me.bristermitten.mittenlib.config.DeserializationContext;
                        import me.bristermitten.mittenlib.config.extension.CustomDeserializerFor;
                        import me.bristermitten.mittenlib.util.Result;
                        
                        @CustomDeserializerFor(CustomType.class)
                        @PassIn
                        public class CustomTypeDeserializer {
                        
                            public static Result<CustomType> deserialize(DeserializationContext context) {
                                return Result.ok(
                                        new CustomType("hello")
                                );
                            }
                        }
                        
                        """)
                .intoUnitTest((processingEnvironment, element) -> {
                    ToolingProvider.setTooling(processingEnvironment);
                    CustomDeserializers customDeserializers = new CustomDeserializers();
                    customDeserializers.registerCustomDeserializer(element);
                })
                .thenExpectThat()
                .compilationSucceeds()
                .executeTest();
    }
}
