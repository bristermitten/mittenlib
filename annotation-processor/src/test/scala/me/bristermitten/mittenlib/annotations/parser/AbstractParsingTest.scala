package me.bristermitten.mittenlib.annotations.parser

import com.google.inject.Guice
import com.palantir.javapoet.ClassName
import io.toolisticon.aptk.common.ToolingProvider
import io.toolisticon.cute.Cute
import javax.lang.model.element.TypeElement
import me.bristermitten.mittenlib.annotations.compile.ConfigProcessorModule
import me.bristermitten.mittenlib.annotations.domain.ConfigStructure
import me.bristermitten.mittenlib.annotations.domain.Property
import me.bristermitten.mittenlib.annotations.integration.InterfaceConfig
import me.bristermitten.mittenlib.annotations.integration.UnionConfig
import org.assertj.core.api.Assertions.assertThat
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import scala.jdk.CollectionConverters.*

class AbstractParsingTest extends AnyFunSuite with Matchers {

  test("testBasicAbstractParsing") {
    Cute
      .unitTest()
      .when()
      .passInElement()
      .fromClass[TypeElement](classOf[InterfaceConfig])
      .intoUnitTest((processingEnvironment, element) => {
        val injector =
          Guice.createInjector(new ConfigProcessorModule(processingEnvironment))
        val ast = injector
          .getInstance(classOf[ConfigParser])
          .getParsedStructure(element)

        assertThat(ast).isNotNull()
        ast shouldBe a[ConfigStructure.Atomic]
        assertThat(ast.name).isEqualTo(ClassName.get(classOf[InterfaceConfig]))

        val enclosed = ast.enclosed.asJava
        assertThat(enclosed).hasSize(1)
        val child = enclosed.get(0).asInstanceOf[ConfigStructure.Atomic]
        assertThat(child.name).isEqualTo(
          ClassName.get(classOf[InterfaceConfig.ChildConfig])
        )

        val childProperties = child.properties.asJava
        assertThat(childProperties).hasSize(1)
        assertThat(childProperties.get(0).name).isEqualTo("id")
      })
      .thenExpectThat()
      .compilationSucceeds()
      .executeTest()
  }

  test("testUnionAbstractParsing") {
    Cute
      .unitTest()
      .when()
      .passInElement()
      .fromClass[TypeElement](classOf[UnionConfig])
      .intoUnitTest((processingEnvironment, element) => {
        ToolingProvider.setTooling(processingEnvironment)
        val injector =
          Guice.createInjector(new ConfigProcessorModule(processingEnvironment))
        val ast = injector
          .getInstance(classOf[ConfigParser])
          .getParsedStructure(element)

        assertThat(ast).isNotNull()
        assertThat(ast.name).isEqualTo(ClassName.get(classOf[UnionConfig]))
        ast shouldBe a[ConfigStructure.Union]
        val union = ast.asInstanceOf[ConfigStructure.Union]

        val alternatives = union.alternatives
        alternatives should have size 2

        val child1 = alternatives.find(
          _.name == ClassName.get(classOf[UnionConfig.Child1Config])
        )
        child1.isDefined shouldBe true
        child1.get.properties
          .map(_.name) should contain theSameElementsAs List("common", "hello")

        val child2 = alternatives.find(
          _.name == ClassName.get(classOf[UnionConfig.Child2Config])
        )
        child2.isDefined shouldBe true
        child2.get.properties
          .map(_.name) should contain theSameElementsAs List("common", "world")

        ast.enclosed should have size 2
      })
      .thenExpectThat()
      .compilationSucceeds()
      .executeTest()
  }
}
