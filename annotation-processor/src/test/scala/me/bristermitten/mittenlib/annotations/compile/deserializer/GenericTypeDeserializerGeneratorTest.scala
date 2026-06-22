package me.bristermitten.mittenlib.annotations.compile.deserializer

import com.google.inject.Guice
import com.palantir.javapoet.TypeName.*
import io.toolisticon.aptk.common.ToolingProvider
import io.toolisticon.aptk.tools.TypeMirrorWrapper
import io.toolisticon.aptk.tools.wrapper.TypeElementWrapper
import io.toolisticon.cute.Cute
import me.bristermitten.mittenlib.annotations.ast.AbstractConfigStructure
import me.bristermitten.mittenlib.annotations.compile.{
  ConfigNameCache,
  ConfigProcessorModule,
  FieldNameGenerator,
  MethodNames
}
import me.bristermitten.mittenlib.annotations.parser.{
  ConfigParser,
  CustomDeserializers
}
import org.assertj.core.api.Assertions.{assertThat, assertThatThrownBy}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import javax.lang.model.element.{TypeElement, VariableElement}
import scala.jdk.CollectionConverters.*

class GenericTypeDeserializerGeneratorTest extends AnyFunSuite with Matchers {

  test("testUnexpectedGenericTypeThrowsException") {
    Cute
      .unitTest()
      .when()
      .passInElement()
      .fromSourceString[TypeElement](
        "me.bristermitten.mittenlib.tests.UnexpectedGenericConfig",
        """
          |package me.bristermitten.mittenlib.tests;
          |import me.bristermitten.mittenlib.config.Config;
          |import io.toolisticon.cute.PassIn;
          |import java.util.concurrent.CompletableFuture;
          |
          |@Config(requireSerialization = false)
          |@PassIn
          |public class UnexpectedGenericConfig {
          |    public CompletableFuture<String> futureField;
          |}
          |""".stripMargin
      )
      .intoUnitTest((processingEnvironment, element) => {
        ToolingProvider.setTooling(processingEnvironment)
        val injector =
          Guice.createInjector(new ConfigProcessorModule(processingEnvironment))
        val generator =
          injector.getInstance(classOf[GenericTypeDeserializerGenerator])
        val parser = injector.getInstance(classOf[ConfigParser])
        val cache = injector.getInstance(classOf[ConfigNameCache])
        val domainAst = parser.getParsedStructure(element)
        val ast: AbstractConfigStructure = cache.lookupAST(domainAst.name).get()

        val field = element.getEnclosedElements
          .stream()
          .filter(e => e.getSimpleName.toString == "futureField")
          .findFirst()
          .orElseThrow()
          .asInstanceOf[VariableElement]

        val wrapper = TypeMirrorWrapper.wrap(field.asType())
        val property = ast.properties().get(0)
        val typeElementWrapper = TypeElementWrapper.wrap(
          processingEnvironment.getTypeUtils
            .asElement(
              processingEnvironment.getTypeUtils.erasure(field.asType())
            )
            .asInstanceOf[TypeElement]
        )

        assertThatThrownBy(() =>
          generator.generateDeserializeMethod(
            ast,
            property,
            element,
            field.asType(),
            wrapper,
            typeElementWrapper,
            get(field.asType()),
            null,
            injector.getInstance(classOf[FieldNameGenerator]),
            injector.getInstance(classOf[MethodNames])
          )
        )
          .hasMessageContaining(
            "Unexpected generic type: java.util.concurrent.CompletableFuture"
          )
      })
      .thenExpectThat()
      .compilationFails()
      .executeTest()
  }

  test("testNestedGenericCollectionsCoverage") {
    Cute
      .unitTest()
      .when()
      .passInElement()
      .fromSourceString[TypeElement](
        "me.bristermitten.mittenlib.tests.NestedGenericConfig",
        """
          |package me.bristermitten.mittenlib.tests;
          |import me.bristermitten.mittenlib.config.Config;
          |import io.toolisticon.cute.PassIn;
          |import java.util.List;
          |import java.util.Map;
          |import java.util.Set;
          |import me.bristermitten.mittenlib.config.extension.CustomDeserializerFor;
          |import me.bristermitten.mittenlib.config.DeserializationContext;
          |import me.bristermitten.mittenlib.util.Result;
          |
          |@Config(requireSerialization = false)
          |@PassIn
          |public class NestedGenericConfig {
          |    public List<List<String>> listOfList;
          |    public Set<Set<Integer>> setOfSet;
          |    public Map<String, Map<String, Boolean>> mapOfMap;
          |    public List<CustomType> listOfCustom;
          |}
          |
          |class CustomType {}
          |
          |@CustomDeserializerFor(CustomType.class)
          |class CustomTypeDeserializer {
          |    public static Result<CustomType> deserialize(DeserializationContext context) {
          |        return Result.ok(new CustomType());
          |    }
          |}
          |""".stripMargin
      )
      .intoUnitTest((processingEnvironment, element) => {
        ToolingProvider.setTooling(processingEnvironment)
        val injector =
          Guice.createInjector(new ConfigProcessorModule(processingEnvironment))
        val generator =
          injector.getInstance(classOf[GenericTypeDeserializerGenerator])
        val parser = injector.getInstance(classOf[ConfigParser])
        val cache = injector.getInstance(classOf[ConfigNameCache])

        val customDeserializers =
          injector.getInstance(classOf[CustomDeserializers])
        val customDeserializerElement = processingEnvironment.getElementUtils
          .getTypeElement(
            "me.bristermitten.mittenlib.tests.CustomTypeDeserializer"
          )
        customDeserializers.registerCustomDeserializer(
          customDeserializerElement
        )

        val domainAst = parser.getParsedStructure(element)
        val ast: AbstractConfigStructure = cache.lookupAST(domainAst.name).get()

        var i = 0
        element.getEnclosedElements
          .stream()
          .filter(e => e.getKind.isField)
          .map(e => e.asInstanceOf[VariableElement])
          .forEach(field => {
            val wrapper = TypeMirrorWrapper.wrap(field.asType())
            val property = ast.properties().get(i)
            i += 1
            val typeElementWrapper = TypeElementWrapper.wrap(
              processingEnvironment.getTypeUtils
                .asElement(
                  processingEnvironment.getTypeUtils.erasure(field.asType())
                )
                .asInstanceOf[TypeElement]
            )

            val result = generator.generateDeserializeMethod(
              ast,
              property,
              element,
              field.asType(),
              wrapper,
              typeElementWrapper,
              get(field.asType()),
              null,
              injector.getInstance(classOf[FieldNameGenerator]),
              injector.getInstance(classOf[MethodNames])
            )
            assertThat(result).isNotNull
            assertThat(result.toString).isNotBlank()
          })
      })
      .thenExpectThat()
      .compilationSucceeds()
      .executeTest()
  }
}
