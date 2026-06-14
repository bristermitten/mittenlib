package me.bristermitten.mittenlib.annotations.config;

import io.toolisticon.cute.Cute;
import org.junit.jupiter.api.Test;

class NewtypeValidationTest {

    @Test
    void testNormalClassWithNewtypeFails() {
        Cute.blackBoxTest()
                .given()
                .processor(ConfigProcessor.class)
                .andSourceFile("InvalidClassNewtype", """
                        package me.bristermitten.mittenlib.tests;

                        import me.bristermitten.mittenlib.config.Newtype;

                        @Newtype
                        public class InvalidClassNewtype {
                            private final String value;
                            public InvalidClassNewtype(String value) {
                                this.value = value;
                            }
                            public String value() { return value; }
                        }
                        """)
                .whenCompiled()
                .thenExpectThat()
                .compilationFails()
                .andThat()
                .compilerMessage()
                .ofKindError()
                .contains(
                        "Newtype annotation only supports records and interfaces: me.bristermitten.mittenlib.tests.InvalidClassNewtype")
                .executeTest();
    }

    @Test
    void testRecordWithZeroComponentsFails() {
        Cute.blackBoxTest()
                .given()
                .processor(ConfigProcessor.class)
                .andSourceFile("EmptyRecordNewtype", """
                        package me.bristermitten.mittenlib.tests;

                        import me.bristermitten.mittenlib.config.Newtype;

                        @Newtype
                        public record EmptyRecordNewtype() {}
                        """)
                .whenCompiled()
                .thenExpectThat()
                .compilationFails()
                .andThat()
                .compilerMessage()
                .ofKindError()
                .contains(
                        "Newtype record me.bristermitten.mittenlib.tests.EmptyRecordNewtype must have exactly one component")
                .executeTest();
    }

    @Test
    void testRecordWithTwoComponentsFails() {
        Cute.blackBoxTest()
                .given()
                .processor(ConfigProcessor.class)
                .andSourceFile("TwoComponentRecordNewtype", """
                        package me.bristermitten.mittenlib.tests;

                        import me.bristermitten.mittenlib.config.Newtype;

                        @Newtype
                        public record TwoComponentRecordNewtype(String first, int second) {}
                        """)
                .whenCompiled()
                .thenExpectThat()
                .compilationFails()
                .andThat()
                .compilerMessage()
                .ofKindError()
                .contains(
                        "Newtype record me.bristermitten.mittenlib.tests.TwoComponentRecordNewtype must have exactly one component")
                .executeTest();
    }

    @Test
    void testInterfaceWithZeroMethodsFails() {
        Cute.blackBoxTest()
                .given()
                .processor(ConfigProcessor.class)
                .andSourceFile("EmptyInterfaceNewtype", """
                        package me.bristermitten.mittenlib.tests;

                        import me.bristermitten.mittenlib.config.Newtype;

                        @Newtype
                        public interface EmptyInterfaceNewtype {}
                        """)
                .whenCompiled()
                .thenExpectThat()
                .compilationFails()
                .andThat()
                .compilerMessage()
                .ofKindError()
                .contains(
                        "Newtype interface me.bristermitten.mittenlib.tests.EmptyInterfaceNewtype must have exactly one abstract method")
                .executeTest();
    }

    @Test
    void testInterfaceWithTwoMethodsFails() {
        Cute.blackBoxTest()
                .given()
                .processor(ConfigProcessor.class)
                .andSourceFile("TwoMethodInterfaceNewtype", """
                        package me.bristermitten.mittenlib.tests;

                        import me.bristermitten.mittenlib.config.Newtype;

                        @Newtype
                        public interface TwoMethodInterfaceNewtype {
                            String first();
                            int second();
                        }
                        """)
                .whenCompiled()
                .thenExpectThat()
                .compilationFails()
                .andThat()
                .compilerMessage()
                .ofKindError()
                .contains(
                        "Newtype interface me.bristermitten.mittenlib.tests.TwoMethodInterfaceNewtype must have exactly one abstract method")
                .executeTest();
    }

    @Test
    void testGenericRecordNewtypeCompiles() {
        Cute.blackBoxTest()
                .given()
                .processor(ConfigProcessor.class)
                .andSourceFile("GenericRecordNewtype", """
                        package me.bristermitten.mittenlib.tests;

                        import me.bristermitten.mittenlib.config.Newtype;

                        @Newtype
                        public record GenericRecordNewtype<T>(String value) {}
                        """)
                .whenCompiled()
                .thenExpectThat()
                .compilationSucceeds()
                .executeTest();
    }

    @Test
    void testGenericInterfaceNewtypeCompiles() {
        Cute.blackBoxTest()
                .given()
                .processor(ConfigProcessor.class)
                .andSourceFile("GenericInterfaceNewtype", """
                        package me.bristermitten.mittenlib.tests;

                        import me.bristermitten.mittenlib.config.Newtype;

                        @Newtype
                        public interface GenericInterfaceNewtype<T> {
                            T value();
                        }
                        """)
                .whenCompiled()
                .thenExpectThat()
                .compilationSucceeds()
                .executeTest();
    }

    @Test
    void testGenericNewtypeConfigCompiles() {
        Cute.blackBoxTest()
                .given()
                .processor(ConfigProcessor.class)
                .andSourceFile("GenericNewtypeConfig", """
                        package me.bristermitten.mittenlib.tests;

                        import me.bristermitten.mittenlib.config.Config;
                        import me.bristermitten.mittenlib.config.Newtype;

                        @Config
                        public interface GenericNewtypeConfig {
                            Id<String> id();

                            @Newtype
                            interface Id<T> {
                                T value();
                            }
                        }
                        """)
                .whenCompiled()
                .thenExpectThat()
                .compilationSucceeds()
                .executeTest();
    }

    @Test
    void testGenericRecordNewtypeConfigCompiles() {
        Cute.blackBoxTest()
                .given()
                .processor(ConfigProcessor.class)
                .andSourceFile("GenericRecordNewtypeConfig", """
                        package me.bristermitten.mittenlib.tests;

                        import me.bristermitten.mittenlib.config.Config;
                        import me.bristermitten.mittenlib.config.Newtype;

                        @Config
                        public interface GenericRecordNewtypeConfig {
                            Id<String> id();

                            @Newtype
                            record Id<T>(T value) {}
                        }
                        """)
                .whenCompiled()
                .thenExpectThat()
                .compilationSucceeds()
                .executeTest();
    }
}
