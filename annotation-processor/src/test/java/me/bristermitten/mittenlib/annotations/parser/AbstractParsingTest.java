package me.bristermitten.mittenlib.annotations.parser;

import com.google.inject.Guice;
import com.squareup.javapoet.ClassName;
import io.toolisticon.aptk.common.ToolingProvider;
import io.toolisticon.cute.Cute;
import me.bristermitten.mittenlib.annotations.ast.ASTParentReference;
import me.bristermitten.mittenlib.annotations.ast.AbstractConfigStructure;
import me.bristermitten.mittenlib.annotations.ast.Property;
import me.bristermitten.mittenlib.annotations.compile.ConfigProcessorModule;
import me.bristermitten.mittenlib.annotations.integration.InterfaceConfig;
import me.bristermitten.mittenlib.annotations.integration.UnionConfig;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import javax.lang.model.element.TypeElement;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class AbstractParsingTest {

    @Test
    void testBasicAbstractParsing() {
        Cute.unitTest()
                .when()
                .passInElement()
                .<TypeElement>fromClass(InterfaceConfig.class)
                .intoUnitTest((processingEnvironment, element) -> {
                    var injector = Guice.createInjector(
                            new ConfigProcessorModule(processingEnvironment)
                    );

                    AbstractConfigStructure ast = injector.getInstance(ConfigClassParser.class)
                            .parseAbstract(element);

                    assertThat(ast)
                            .isNotNull()
                            .isInstanceOf(AbstractConfigStructure.Atomic.class)
                            .extracting(AbstractConfigStructure::name)
                            .isEqualTo(ClassName.get(InterfaceConfig.class));

                    assertThat(ast.enclosedIn())
                            .isNull();

                    assertThat(ast.enclosed())
                            .singleElement()
                            .isNotNull()
                            .asInstanceOf(InstanceOfAssertFactories.type(AbstractConfigStructure.Atomic.class))
                            .hasFieldOrPropertyWithValue("name", ClassName.get(InterfaceConfig.ChildConfig.class))
                            .hasFieldOrPropertyWithValue("enclosed", List.of())
                            .hasFieldOrPropertyWithValue("enclosedIn", new ASTParentReference(ClassName.get(InterfaceConfig.class), null))
                            .extracting(AbstractConfigStructure.Atomic::properties,
                                    InstanceOfAssertFactories.list(Property.class))
                            .singleElement()
                            .hasFieldOrPropertyWithValue("name", "id")
                    ;

                })
                .thenExpectThat()
                .compilationSucceeds()
                .executeTest();
    }

    @Test
    void testUnionAbstractParsing() {
        Cute.unitTest()
                .when()
                .passInElement()
                .<TypeElement>fromClass(UnionConfig.class)
                .intoUnitTest((processingEnvironment, element) -> {
                    ToolingProvider.setTooling(processingEnvironment);
                    var injector = Guice.createInjector(
                            new ConfigProcessorModule(processingEnvironment)
                    );

                    AbstractConfigStructure ast = injector.getInstance(ConfigClassParser.class)
                            .parseAbstract(element);

                    assertThat(ast).isNotNull();
                    assertThat(ast.name())
                            .isEqualTo(ClassName.get(UnionConfig.class));

                    assertThat(ast)
                            .asInstanceOf(InstanceOfAssertFactories.type(AbstractConfigStructure.Union.class))
                            .extracting(AbstractConfigStructure.Union::alternatives)
                            .asInstanceOf(InstanceOfAssertFactories.list(AbstractConfigStructure.class))
                            .filteredOn(p -> p.name().equals(ClassName.get(UnionConfig.Child1Config.class)))
                            .singleElement()
                            .extracting(AbstractConfigStructure::properties)
                            .asInstanceOf(InstanceOfAssertFactories.list(Property.class))
                            .satisfiesOnlyOnce(c -> assertThat(c.name()).isEqualTo("hello"));


                    assertThat(ast)
                            .asInstanceOf(InstanceOfAssertFactories.type(AbstractConfigStructure.Union.class))
                            .extracting(AbstractConfigStructure.Union::alternatives)
                            .asInstanceOf(InstanceOfAssertFactories.list(AbstractConfigStructure.class))
                            .filteredOn(p -> p.name().equals(ClassName.get(UnionConfig.Child2Config.class)))
                            .singleElement()
                            .extracting(AbstractConfigStructure::properties)
                            .asInstanceOf(InstanceOfAssertFactories.list(Property.class))
                            .satisfiesOnlyOnce(c -> assertThat(c.name()).isEqualTo("world"));


                    assertThat(ast.enclosedIn()).isNull();

                    assertThat(ast.enclosed()).hasSize(2);

                })
                .thenExpectThat().compilationSucceeds().executeTest();

    }
}
