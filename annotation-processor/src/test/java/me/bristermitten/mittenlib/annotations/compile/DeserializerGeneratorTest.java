package me.bristermitten.mittenlib.annotations.compile;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import me.bristermitten.mittenlib.annotations.config.ConfigProcessor;
import org.junit.jupiter.api.Test;

class DeserializerGeneratorTest {

    @Test
    void testCompilationWithCustomDeserializersAndEnums() {
        Compilation compilation = javac().withProcessors(new ConfigProcessor())
                .compile(
                        JavaFileObjects.forSourceString("me.bristermitten.mittenlib.tests.CustomType1", """
                        package me.bristermitten.mittenlib.tests;

                        public class CustomType1 {}
                        """),
                        JavaFileObjects.forSourceString("me.bristermitten.mittenlib.tests.CustomType2", """
                        package me.bristermitten.mittenlib.tests;

                        public class CustomType2 {}
                        """),
                        JavaFileObjects.forSourceString("me.bristermitten.mittenlib.tests.TestEnum", """
                        package me.bristermitten.mittenlib.tests;

                        public enum TestEnum {
                            ONE, TWO
                        }
                        """),
                        JavaFileObjects.forSourceString(
                                "me.bristermitten.mittenlib.tests.CustomType1Deserializer", """
                        package me.bristermitten.mittenlib.tests;

                        import me.bristermitten.mittenlib.config.DeserializationContext;
                        import me.bristermitten.mittenlib.config.extension.CustomDeserializerFor;
                        import me.bristermitten.mittenlib.util.Result;

                        @CustomDeserializerFor(CustomType1.class)
                        public class CustomType1Deserializer {
                            public static Result<CustomType1> deserialize(DeserializationContext context) {
                                return Result.ok(new CustomType1());
                            }
                        }
                        """),
                        JavaFileObjects.forSourceString(
                                "me.bristermitten.mittenlib.tests.CustomType2Deserializer", """
                        package me.bristermitten.mittenlib.tests;

                        import me.bristermitten.mittenlib.config.DeserializationContext;
                        import me.bristermitten.mittenlib.config.extension.CustomDeserializerFor;
                        import me.bristermitten.mittenlib.config.extension.CustomDeserializer;
                        import me.bristermitten.mittenlib.util.Result;

                        @CustomDeserializerFor(CustomType2.class)
                        public class CustomType2Deserializer implements CustomDeserializer<CustomType2> {
                            @Override
                            public Result<CustomType2> apply(DeserializationContext context) {
                                return Result.ok(new CustomType2());
                            }
                        }
                        """),
                        JavaFileObjects.forSourceString("me.bristermitten.mittenlib.tests.CustomType2Serializer", """
                        package me.bristermitten.mittenlib.tests;

                        import me.bristermitten.mittenlib.config.SerializationContext;
                        import me.bristermitten.mittenlib.config.extension.CustomSerializerFor;
                        import me.bristermitten.mittenlib.config.extension.CustomSerializer;
                        import me.bristermitten.mittenlib.config.tree.DataTree;

                        @CustomSerializerFor(CustomType2.class)
                        public class CustomType2Serializer implements CustomSerializer<CustomType2> {
                            @Override
                            public DataTree apply(CustomType2 value, SerializationContext context) {
                                return DataTree.string("custom2");
                            }
                        }
                        """),
                        JavaFileObjects.forSourceString(
                                "me.bristermitten.mittenlib.tests.DeserializerTestConfigDTO", """
                        package me.bristermitten.mittenlib.tests;

                        import me.bristermitten.mittenlib.config.Config;
                        import me.bristermitten.mittenlib.config.EnumParsingScheme;
                        import me.bristermitten.mittenlib.config.EnumParsingSchemes;
                        import java.util.List;
                        import java.util.Map;
                        import java.util.Set;

                        @Config(requireSerialization = false)
                        public class DeserializerTestConfigDTO {
                            public CustomType1 directStaticCustom;
                            public CustomType2 directNonStaticCustom;

                            public List<CustomType1> listStaticCustom;
                            public List<CustomType2> listNonStaticCustom;

                            // Nested collections and complex generics
                            public Map<String, List<CustomType1>> nestedMapWithStaticCustom;
                            public Set<List<CustomType2>> nestedSetWithNonStaticCustom;

                            @EnumParsingScheme(EnumParsingSchemes.EXACT_MATCH)
                            public TestEnum exactEnum;

                            @EnumParsingScheme(EnumParsingSchemes.CASE_INSENSITIVE)
                            public TestEnum lowercaseEnum;
                        }
                        """));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("me.bristermitten.mittenlib.tests.DeserializerTestConfigDeserializer");
    }
}
