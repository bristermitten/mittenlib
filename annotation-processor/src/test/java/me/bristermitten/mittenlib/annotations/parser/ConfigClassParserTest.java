package me.bristermitten.mittenlib.annotations.parser;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThatCollection;

import com.google.inject.Guice;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.JavaFile;
import io.toolisticon.aptk.common.ToolingProvider;
import io.toolisticon.cute.Cute;
import io.toolisticon.cute.PassIn;
import javax.lang.model.element.TypeElement;
import me.bristermitten.mittenlib.annotations.ast.AbstractConfigStructure;
import me.bristermitten.mittenlib.annotations.compile.ConfigImplGenerator;
import me.bristermitten.mittenlib.annotations.compile.ConfigProcessorModule;
import me.bristermitten.mittenlib.annotations.integration.AtomicConfig;
import me.bristermitten.mittenlib.annotations.integration.InterfaceConfig;
import me.bristermitten.mittenlib.annotations.integration.IntersectionConfig;
import me.bristermitten.mittenlib.annotations.integration.UnionConfig;
import me.bristermitten.mittenlib.config.Config;
import me.bristermitten.mittenlib.config.names.ConfigName;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ConfigClassParserTest {

    @Test
    void testParsingAtomicConfig() {
        Cute.unitTest()
                .when()
                .passInElement()
                .<TypeElement>fromClass(AtomicConfig.class)
                .intoUnitTest((processingEnvironment, element) -> {
                    var injector = Guice.createInjector(new ConfigProcessorModule(processingEnvironment));

                    AbstractConfigStructure ast =
                            injector.getInstance(ConfigClassParser.class).parseAbstract(element);

                    assertThat(ast).isNotNull().isInstanceOf(AbstractConfigStructure.Atomic.class);
                })
                .thenExpectThat()
                .compilationSucceeds()
                .executeTest();
    }

    @Test
    void testParsingIntersectionConfig() {
        Cute.unitTest()
                .when()
                .passInElement()
                .<TypeElement>fromClass(IntersectionConfig.class)
                .intoUnitTest((processingEnvironment, element) -> {
                    var injector = Guice.createInjector(new ConfigProcessorModule(processingEnvironment));
                    AbstractConfigStructure ast =
                            injector.getInstance(ConfigClassParser.class).parseAbstract(element);

                    assertThat(ast).isNotNull();
                    assertThat(ast).isNotNull().isInstanceOf(AbstractConfigStructure.Atomic.class);

                    assertThatCollection(ast.enclosed())
                            .singleElement()
                            //                            .extracting(ConfigTypeAST::structure)
                            .isInstanceOf(AbstractConfigStructure.Intersection.class)
                            .extracting(AbstractConfigStructure::name)
                            .isEqualTo(ClassName.get(IntersectionConfig.ChildIntersectionConfig.class));
                })
                .thenExpectThat()
                .compilationSucceeds()
                .executeTest();
    }

    @Test
    void testParsingNestedConfig() {
        Cute.unitTest()
                .when()
                .passInElement()
                .<TypeElement>fromClass(InterfaceConfig.class)
                .intoUnitTest((processingEnvironment, element) -> {
                    var injector = Guice.createInjector(new ConfigProcessorModule(processingEnvironment));
                    AbstractConfigStructure ast =
                            injector.getInstance(ConfigClassParser.class).parseAbstract(element);

                    assertThat(ast).isNotNull().isInstanceOf(AbstractConfigStructure.Atomic.class);

                    assertThatCollection(ast.enclosed()).first().isInstanceOf(AbstractConfigStructure.Atomic.class);
                })
                .thenExpectThat()
                .compilationSucceeds()
                .executeTest();
    }

    @Test
    void testParsingUnionConfig() {
        Cute.unitTest()
                .when()
                .passInElement()
                .<TypeElement>fromClass(UnionConfig.class)
                .intoUnitTest((processingEnvironment, element) -> {
                    var injector = Guice.createInjector(new ConfigProcessorModule(processingEnvironment));
                    AbstractConfigStructure ast =
                            injector.getInstance(ConfigClassParser.class).parseAbstract(element);

                    assertThat(ast).isNotNull();
                    assertThat(ast)
                            .isNotNull()
                            .asInstanceOf(InstanceOfAssertFactories.type(AbstractConfigStructure.Union.class))
                            .extracting(AbstractConfigStructure.Union::alternatives)
                            .asInstanceOf(InstanceOfAssertFactories.list(AbstractConfigStructure.class))
                            .hasSize(2)
                            .first()
                            .isInstanceOf(
                                    AbstractConfigStructure.Intersection.class) // since it extends the parent type
                            .isNotNull();
                })
                .thenExpectThat()
                .compilationSucceeds()
                .executeTest();
    }

    @Test
    void testParsingBasicConfig() {
        Cute.unitTest()
                .when()
                .passInElement()
                .<TypeElement>fromClass(TestInterfaceConfig.class)
                .intoUnitTest((processingEnvironment, element) -> {
                    ToolingProvider.setTooling(processingEnvironment);
                    var injector = Guice.createInjector(new ConfigProcessorModule(processingEnvironment));

                    AbstractConfigStructure ast =
                            injector.getInstance(ConfigClassParser.class).parseAbstract(element);

                    //                    assertThat(ast)
                    //                            .extracting(ConfigTypeAST::structure)
                    //                            .extracting(ConfigTypeAST.ConfigStructure::name)
                    //                            .extracting(ClassName::simpleName)
                    //                            .isEqualTo(TestInterfaceConfig.class.getSimpleName());
                    //
                    //                    assertThatList(ast.structure().properties())
                    //                            .hasSize(2);
                    //
                    //                    assertThatList(ast.structure().properties())
                    //                            .first()
                    //                            .extracting(Property::name)
                    //                            .isEqualTo("name");
                    //
                    //                    assertThatList(ast.structure().properties())
                    //                            .first()
                    //                            .extracting(Property::settings)
                    //                            .extracting(ASTSettings.PropertyASTSettings::configName)
                    //                            .extracting(ConfigName::value)
                    //                            .isEqualTo("thing-name");

                    var generator = injector.getInstance(ConfigImplGenerator.class);
                    JavaFile emit = generator.emit(ast);

                    assertThat(emit).isNotNull();
                    assertThat(emit.typeSpec().name()).isEqualTo("TestInterfaceConfigImpl");
                })
                .thenExpectThat()
                .compilationSucceeds()
                .executeTest();
    }

    @SuppressWarnings("unused")
    @Config
    @PassIn
    private interface TestInterfaceConfig {
        @ConfigName("thing-name")
        String name();

        int age();
    }

    @Test
    void testParsingConfigWithMultipleParents() {
        Cute.unitTest()
                .when()
                .passInElement()
                .<TypeElement>fromSourceString("me.bristermitten.mittenlib.tests.MultiParentConfigDTO", """
                        package me.bristermitten.mittenlib.tests;
                        import io.toolisticon.cute.PassIn;
                        import me.bristermitten.mittenlib.config.Config;

                        interface SomeInterface {}
                        class SuperClass {}

                        @Config
                        @PassIn
                        public class MultiParentConfigDTO extends SuperClass implements SomeInterface {
                            public int level;
                        }
                        """)
                .intoUnitTest((processingEnvironment, element) -> {
                    var injector = Guice.createInjector(new ConfigProcessorModule(processingEnvironment));
                    var parser = injector.getInstance(ConfigClassParser.class);
                    Assertions.assertThrows(IllegalArgumentException.class, () -> parser.parseAbstract(element));
                })
                .thenExpectThat()
                .compilationSucceeds()
                .executeTest();
    }

    @Test
    void testParsingConfigEnum() {
        Cute.unitTest()
                .when()
                .passInElement()
                .<TypeElement>fromSourceString("me.bristermitten.mittenlib.tests.EnumConfigDTO", """
                        package me.bristermitten.mittenlib.tests;
                        import io.toolisticon.cute.PassIn;
                        import me.bristermitten.mittenlib.config.Config;

                        @Config
                        @PassIn
                        public enum EnumConfigDTO {
                            VAL
                        }
                        """)
                .intoUnitTest((processingEnvironment, element) -> {
                    var injector = Guice.createInjector(new ConfigProcessorModule(processingEnvironment));
                    var parser = injector.getInstance(ConfigClassParser.class);
                    Assertions.assertThrows(IllegalArgumentException.class, () -> parser.parseAbstract(element));
                })
                .thenExpectThat()
                .compilationSucceeds()
                .executeTest();
    }
}
