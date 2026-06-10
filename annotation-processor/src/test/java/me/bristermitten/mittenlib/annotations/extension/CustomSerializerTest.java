package me.bristermitten.mittenlib.annotations.extension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.toolisticon.aptk.common.ToolingProvider;
import io.toolisticon.cute.Cute;
import javax.lang.model.element.TypeElement;
import me.bristermitten.mittenlib.annotations.parser.CustomSerializers;
import org.junit.jupiter.api.Test;

class CustomSerializerTest {

    @Test
    void testSuccessfulRegistrationStaticMethod() {
        Cute.unitTest()
                .when()
                .passInElement()
                .<TypeElement>fromSourceString("me.bristermitten.mittenlib.annotations.extension.TestSerializer", """
                        import io.toolisticon.cute.PassIn;
                        import me.bristermitten.mittenlib.config.SerializationContext;
                        import me.bristermitten.mittenlib.config.extension.CustomSerializerFor;
                        import me.bristermitten.mittenlib.config.tree.DataTree;

                        @CustomSerializerFor(String.class)
                        @PassIn
                        public class TestSerializer {
                            public static DataTree serialize(String value, SerializationContext context) {
                                return DataTree.string("serialized-" + value);
                            }
                        }
                        """)
                .intoUnitTest((processingEnvironment, element) -> {
                    ToolingProvider.setTooling(processingEnvironment);
                    CustomSerializers customSerializers = new CustomSerializers();
                    customSerializers.registerCustomSerializer(element);
                })
                .thenExpectThat()
                .compilationSucceeds()
                .executeTest();
    }

    @Test
    void testSuccessfulRegistrationInterface() {
        Cute.unitTest()
                .when()
                .passInElement()
                .<TypeElement>fromSourceString("me.bristermitten.mittenlib.annotations.extension.TestSerializer", """
                        import io.toolisticon.cute.PassIn;
                        import me.bristermitten.mittenlib.config.SerializationContext;
                        import me.bristermitten.mittenlib.config.extension.CustomSerializer;
                        import me.bristermitten.mittenlib.config.extension.CustomSerializerFor;
                        import me.bristermitten.mittenlib.config.tree.DataTree;

                        @CustomSerializerFor(String.class)
                        @PassIn
                        public class TestSerializer implements CustomSerializer<String> {
                            @Override
                            public DataTree apply(String value, SerializationContext context) {
                                return DataTree.string("serialized-" + value);
                            }
                        }
                        """)
                .intoUnitTest((processingEnvironment, element) -> {
                    ToolingProvider.setTooling(processingEnvironment);
                    CustomSerializers customSerializers = new CustomSerializers();
                    customSerializers.registerCustomSerializer(element);
                })
                .thenExpectThat()
                .compilationSucceeds()
                .executeTest();
    }

    @Test
    void testMissingAnnotationFails() {
        Cute.unitTest()
                .when()
                .passInElement()
                .<TypeElement>fromSourceString("me.bristermitten.mittenlib.annotations.extension.TestSerializer", """
                        import io.toolisticon.cute.PassIn;
                        import me.bristermitten.mittenlib.config.SerializationContext;
                        import me.bristermitten.mittenlib.config.tree.DataTree;

                        @PassIn
                        public class TestSerializer {
                            public static DataTree serialize(String value, SerializationContext context) {
                                return DataTree.string("serialized-" + value);
                            }
                        }
                        """)
                .intoUnitTest((processingEnvironment, element) -> {
                    ToolingProvider.setTooling(processingEnvironment);
                    CustomSerializers customSerializers = new CustomSerializers();
                    assertThatThrownBy(() -> customSerializers.registerCustomSerializer(element))
                            .isInstanceOf(IllegalArgumentException.class)
                            .hasMessageContaining("CustomSerializer must be annotated with @CustomSerializerFor");
                })
                .thenExpectThat()
                .compilationSucceeds()
                .executeTest();
    }

    @Test
    void testMissingInterfaceOrMethodFails() {
        Cute.unitTest()
                .when()
                .passInElement()
                .<TypeElement>fromSourceString("me.bristermitten.mittenlib.annotations.extension.TestSerializer", """
                        import io.toolisticon.cute.PassIn;
                        import me.bristermitten.mittenlib.config.extension.CustomSerializerFor;

                        @CustomSerializerFor(String.class)
                        @PassIn
                        public class TestSerializer {
                            // Missing both serialize method and CustomSerializer interface
                        }
                        """)
                .intoUnitTest((processingEnvironment, element) -> {
                    ToolingProvider.setTooling(processingEnvironment);
                    CustomSerializers customSerializers = new CustomSerializers();
                    assertThatThrownBy(() -> customSerializers.registerCustomSerializer(element))
                            .isInstanceOf(IllegalArgumentException.class)
                            .hasMessageContaining(
                                    "CustomSerializer must implement CustomSerializer or have a static method DataTree serialize(T, SerializationContext)");
                })
                .thenExpectThat()
                .compilationSucceeds()
                .executeTest();
    }

    @Test
    void testWrongReturnTypeMethodFails() {
        Cute.unitTest()
                .when()
                .passInElement()
                .<TypeElement>fromSourceString("me.bristermitten.mittenlib.annotations.extension.TestSerializer", """
                        import io.toolisticon.cute.PassIn;
                        import me.bristermitten.mittenlib.config.SerializationContext;
                        import me.bristermitten.mittenlib.config.extension.CustomSerializerFor;

                        @CustomSerializerFor(String.class)
                        @PassIn
                        public class TestSerializer {
                            public static String serialize(String value, SerializationContext context) {
                                return "serialized-" + value;
                            }
                        }
                        """)
                .intoUnitTest((processingEnvironment, element) -> {
                    ToolingProvider.setTooling(processingEnvironment);
                    CustomSerializers customSerializers = new CustomSerializers();
                    customSerializers.registerCustomSerializer(element);
                })
                .thenExpectThat()
                .compilationFails()
                .andThat()
                .compilerMessage()
                .ofKindError()
                .contains("Custom serializer method must return DataTree")
                .executeTest();
    }

    @Test
    void testNonStaticMethodWithoutInterfaceFails() {
        Cute.unitTest()
                .when()
                .passInElement()
                .<TypeElement>fromSourceString("me.bristermitten.mittenlib.annotations.extension.TestSerializer", """
                        import io.toolisticon.cute.PassIn;
                        import me.bristermitten.mittenlib.config.SerializationContext;
                        import me.bristermitten.mittenlib.config.extension.CustomSerializerFor;
                        import me.bristermitten.mittenlib.config.tree.DataTree;

                        @CustomSerializerFor(String.class)
                        @PassIn
                        public class TestSerializer {
                            // Method is not static, and class doesn't implement CustomSerializer
                            public DataTree serialize(String value, SerializationContext context) {
                                return DataTree.string("serialized-" + value);
                            }
                        }
                        """)
                .intoUnitTest((processingEnvironment, element) -> {
                    ToolingProvider.setTooling(processingEnvironment);
                    CustomSerializers customSerializers = new CustomSerializers();
                    customSerializers.registerCustomSerializer(element);
                })
                .thenExpectThat()
                .compilationFails()
                .andThat()
                .compilerMessage()
                .ofKindError()
                .contains("Non static custom serializers must implement CustomSerializer")
                .executeTest();
    }
}
