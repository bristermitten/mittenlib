package me.bristermitten.mittenlib.annotations.compile.deserializer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.palantir.javapoet.MethodSpec;
import io.toolisticon.aptk.common.ToolingProvider;
import io.toolisticon.aptk.tools.TypeMirrorWrapper;
import io.toolisticon.aptk.tools.wrapper.TypeElementWrapper;
import io.toolisticon.cute.Cute;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import me.bristermitten.mittenlib.annotations.ast.ASTSettings;
import me.bristermitten.mittenlib.annotations.ast.Property;
import me.bristermitten.mittenlib.annotations.compile.ConfigNameCache;
import me.bristermitten.mittenlib.annotations.compile.ConfigurationClassNameGenerator;
import me.bristermitten.mittenlib.annotations.compile.GeneratedTypeCache;
import me.bristermitten.mittenlib.annotations.parser.CustomDeserializers;
import me.bristermitten.mittenlib.annotations.util.TypesUtil;
import me.bristermitten.mittenlib.config.EnumParsingSchemes;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

class GenericTypeDeserializerGeneratorTest {

    @Test
    void testUnexpectedGenericTypeThrowsException() {
        Cute.unitTest()
                .when()
                .passInElement()
                .<TypeElement>fromSourceString("me.bristermitten.mittenlib.tests.UnexpectedGenericConfig", """
                        package me.bristermitten.mittenlib.tests;
                        import me.bristermitten.mittenlib.config.Config;
                        import io.toolisticon.cute.PassIn;
                        import java.util.Optional;

                        @Config
                        @PassIn
                        public class UnexpectedGenericConfig {
                            public Optional<String> optionalField;
                        }
                        """)
                .intoUnitTest((processingEnvironment, element) -> {
                    ToolingProvider.setTooling(processingEnvironment);
                    GenericTypeDeserializerGenerator generator = getDeserializerGenerator(processingEnvironment);

                    VariableElement field = (VariableElement) element.getEnclosedElements().stream()
                            .filter(e -> e.getSimpleName().toString().equals("optionalField"))
                            .findFirst()
                            .orElseThrow();

                    TypeMirrorWrapper wrapper = TypeMirrorWrapper.wrap(field.asType());
                    Property property = new Property(
                            "optionalField",
                            field.asType(),
                            new Property.PropertySource.FieldSource(field),
                            new ASTSettings.PropertyASTSettings(
                                    null,
                                    null,
                                    EnumParsingSchemes.EXACT_MATCH,
                                    false,
                                    false,
                                    List.of(),
                                    List.of(),
                                    List.of()));
                    TypeElementWrapper typeElementWrapper = TypeElementWrapper.wrap(
                            (TypeElement) processingEnvironment.getTypeUtils().asElement(field.asType()));

                    assertThatThrownBy(() ->
                                    generator.handleGenericType(null, element, property, wrapper, typeElementWrapper))
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("Unexpected generic type: java.util.Optional");
                })
                .thenExpectThat()
                .compilationFails()
                .executeTest();
    }

    private static @NonNull GenericTypeDeserializerGenerator getDeserializerGenerator(
            ProcessingEnvironment processingEnvironment) {
        TypesUtil typesUtil = new TypesUtil(
                processingEnvironment.getTypeUtils(),
                processingEnvironment.getElementUtils(),
                new GeneratedTypeCache());
        ConfigurationClassNameGenerator classNameGenerator = new ConfigurationClassNameGenerator(new ConfigNameCache());

        GenericTypeDeserializerGenerator generator = new GenericTypeDeserializerGenerator(
                typesUtil,
                classNameGenerator,
                new CustomDeserializers(),
                new NonGenericTypeDeserializerGenerator(typesUtil, classNameGenerator, new CustomDeserializers()));
        return generator;
    }

    @Test
    void testNestedGenericCollectionsCoverage() {
        Cute.unitTest()
                .when()
                .passInElement()
                .<TypeElement>fromSourceString("me.bristermitten.mittenlib.tests.NestedGenericConfig", """
                        package me.bristermitten.mittenlib.tests;
                        import me.bristermitten.mittenlib.config.Config;
                        import io.toolisticon.cute.PassIn;
                        import java.util.List;
                        import java.util.Map;
                        import java.util.Set;
                        import me.bristermitten.mittenlib.config.extension.CustomDeserializerFor;
                        import me.bristermitten.mittenlib.config.DeserializationContext;
                        import me.bristermitten.mittenlib.util.Result;

                        @Config
                        @PassIn
                        public class NestedGenericConfig {
                            public List<List<String>> listOfList;
                            public Set<Set<Integer>> setOfSet;
                            public Map<String, Map<String, Boolean>> mapOfMap;
                            public List<CustomType> listOfCustom;
                        }

                        class CustomType {}

                        @CustomDeserializerFor(CustomType.class)
                        class CustomTypeDeserializer {
                            public static Result<CustomType> deserialize(DeserializationContext context) {
                                return Result.ok(new CustomType());
                            }
                        }
                        """)
                .intoUnitTest((processingEnvironment, element) -> {
                    ToolingProvider.setTooling(processingEnvironment);
                    TypesUtil typesUtil = new TypesUtil(
                            processingEnvironment.getTypeUtils(),
                            processingEnvironment.getElementUtils(),
                            new GeneratedTypeCache());
                    ConfigurationClassNameGenerator classNameGenerator =
                            new ConfigurationClassNameGenerator(new ConfigNameCache());

                    CustomDeserializers customDeserializers = new CustomDeserializers();
                    TypeElement customDeserializerElement = processingEnvironment
                            .getElementUtils()
                            .getTypeElement("me.bristermitten.mittenlib.tests.CustomTypeDeserializer");
                    customDeserializers.registerCustomDeserializer(customDeserializerElement);

                    GenericTypeDeserializerGenerator generator = new GenericTypeDeserializerGenerator(
                            typesUtil,
                            classNameGenerator,
                            customDeserializers,
                            new NonGenericTypeDeserializerGenerator(
                                    typesUtil, classNameGenerator, customDeserializers));

                    for (VariableElement field : element.getEnclosedElements().stream()
                            .filter(e -> e.getKind().isField())
                            .map(VariableElement.class::cast)
                            .toList()) {
                        TypeMirrorWrapper wrapper = TypeMirrorWrapper.wrap(field.asType());
                        Property property = new Property(
                                field.getSimpleName().toString(),
                                field.asType(),
                                new Property.PropertySource.FieldSource(field),
                                new ASTSettings.PropertyASTSettings(
                                        null,
                                        null,
                                        EnumParsingSchemes.EXACT_MATCH,
                                        false,
                                        false,
                                        List.of(),
                                        List.of(),
                                        List.of()));
                        TypeElementWrapper typeElementWrapper =
                                TypeElementWrapper.wrap((TypeElement) processingEnvironment
                                        .getTypeUtils()
                                        .asElement(processingEnvironment
                                                .getTypeUtils()
                                                .erasure(field.asType())));

                        var builder = MethodSpec.methodBuilder("temp");
                        var result =
                                generator.handleGenericType(builder, element, property, wrapper, typeElementWrapper);
                        assertThat(result).isPresent();
                        assertThat(builder.build().code().toString()).isNotBlank();
                    }
                })
                .thenExpectThat()
                .compilationSucceeds()
                .executeTest();
    }
}
