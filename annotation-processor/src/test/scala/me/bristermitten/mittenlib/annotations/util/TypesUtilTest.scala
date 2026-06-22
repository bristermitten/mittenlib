package me.bristermitten.mittenlib.annotations.util

import com.palantir.javapoet.ClassName
import com.palantir.javapoet.ParameterizedTypeName
import com.palantir.javapoet.TypeName
import io.toolisticon.aptk.common.ToolingProvider
import io.toolisticon.cute.Cute
import java.util.List
import java.util.Map
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.`type`.TypeKind
import javax.lang.model.`type`.TypeMirror
import me.bristermitten.mittenlib.annotations.compile.GeneratedTypeCache
import me.bristermitten.mittenlib.annotations.exception.DTOReferenceException
import org.assertj.core.api.Assertions.assertThat
import org.mockito.Mockito.*
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class TypesUtilTest extends AnyFunSuite with Matchers {

  test("testTypesUtil") {
    Cute
      .unitTest()
      .when()
      .passInElement()
      .fromSourceString[TypeElement](
        "me.bristermitten.mittenlib.tests.TestConfigClass",
        """
          |package me.bristermitten.mittenlib.tests;
          |import io.toolisticon.cute.PassIn;
          |import me.bristermitten.mittenlib.config.Config;
          |import me.bristermitten.mittenlib.annotations.util.IntegrationTests.CascadingAnnotation;
          |import org.jspecify.annotations.Nullable;
          |
          |@Config
          |@CascadingAnnotation
          |@PassIn
          |public class TestConfigClass {
          |    @Nullable String nullableField;
          |    int nonNullableField;
          |
          |    @Nullable String nullableMethod() {
          |        return null;
          |    }
          |
          |    String nonNullableMethod() {
          |        return "";
          |    }
          |
          |    @Config
          |    public static class InnerClass {}
          |
          |    public static class NonConfigClass {}
          |}
          |""".stripMargin
      )
      .intoUnitTest((processingEnvironment, element) => {
        ToolingProvider.setTooling(processingEnvironment)
        val typesUtil = new TypesUtil(
          processingEnvironment.getTypeUtils,
          processingEnvironment.getElementUtils,
          new GeneratedTypeCache()
        )

        // 1. getSafeType and getBoxedType
        val intType =
          processingEnvironment.getTypeUtils.getPrimitiveType(TypeKind.INT)
        assertThat(typesUtil.getSafeType(intType).toString)
          .isEqualTo("java.lang.Integer")
        assertThat(typesUtil.getBoxedType(intType).toString)
          .isEqualTo("java.lang.Integer")

        val stringType = processingEnvironment.getElementUtils
          .getTypeElement("java.lang.String")
          .asType()
        assertThat(typesUtil.getSafeType(stringType)).isEqualTo(stringType)
        assertThat(typesUtil.getBoxedType(stringType)).isEqualTo(stringType)

        // 2. Nullability checks on Element
        var nullableField: VariableElement = null
        var nonNullableField: VariableElement = null
        var nullableMethod: ExecutableElement = null
        var nonNullableMethod: ExecutableElement = null

        import scala.jdk.CollectionConverters.*
        for (enclosed <- element.getEnclosedElements.asScala) {
          enclosed match {
            case v: VariableElement =>
              if (v.getSimpleName.toString == "nullableField") {
                nullableField = v
              } else if (v.getSimpleName.toString == "nonNullableField") {
                nonNullableField = v
              }
            case e: ExecutableElement =>
              if (e.getSimpleName.toString == "nullableMethod") {
                nullableMethod = e
              } else if (e.getSimpleName.toString == "nonNullableMethod") {
                nonNullableMethod = e
              }
            case _ =>
          }
        }

        assertThat(nullableField).isNotNull()
        assertThat(nonNullableField).isNotNull()
        assertThat(nullableMethod).isNotNull()
        assertThat(nonNullableMethod).isNotNull()

        // 3. Nullability checks on TypeMirror
        assertThat(typesUtil.isNullable(intType)).isFalse()

        // 4. getAnnotation and Cascading
        assertThat(
          typesUtil.getAnnotation(
            element,
            classOf[IntegrationTests.CascadingAnnotation]
          )
        ).isNotNull()

        // Check inner class inherits CascadingAnnotation
        var innerElement: TypeElement = null
        for (enclosed <- element.getEnclosedElements.asScala) {
          enclosed match {
            case t: TypeElement if t.getSimpleName.toString == "InnerClass" =>
              innerElement = t
            case _ =>
          }
        }
        assertThat(innerElement).isNotNull()
        assertThat(
          typesUtil.getAnnotation(
            innerElement,
            classOf[IntegrationTests.CascadingAnnotation]
          )
        ).isNotNull()

        // Enclosing element null case
        val topLevelElement = processingEnvironment.getElementUtils
          .getTypeElement("java.lang.String")
        assertThat(
          typesUtil.getAnnotation(
            topLevelElement,
            classOf[IntegrationTests.CascadingAnnotation]
          )
        ).isNull()

        // 5. isConfigType
        assertThat(typesUtil.isConfigType(element.asType())).isTrue()

        val nonConfigType = processingEnvironment.getElementUtils
          .getTypeElement(
            "me.bristermitten.mittenlib.tests.TestConfigClass.NonConfigClass"
          )
          .asType()
        assertThat(typesUtil.isConfigType(nonConfigType)).isFalse()

        // non-DeclaredType ConfigType check
        assertThat(typesUtil.isConfigType(intType)).isFalse()

        // 6. getDataTreeType checks
        typesUtil.getDataTreeType(TypeName.INT).isDefined shouldBe true
        typesUtil.getDataTreeType(TypeName.LONG).isDefined shouldBe true
        typesUtil.getDataTreeType(TypeName.SHORT).isDefined shouldBe true
        typesUtil.getDataTreeType(TypeName.BYTE).isDefined shouldBe true
        typesUtil.getDataTreeType(TypeName.FLOAT).isDefined shouldBe true
        typesUtil.getDataTreeType(TypeName.DOUBLE).isDefined shouldBe true
        typesUtil.getDataTreeType(TypeName.BOOLEAN).isDefined shouldBe true
        typesUtil
          .getDataTreeType(ClassName.get(classOf[String]))
          .isDefined shouldBe true

        val mapTypeName = ParameterizedTypeName
          .get(classOf[Map[_, _]], classOf[String], classOf[Integer])
        typesUtil.getDataTreeType(mapTypeName).isDefined shouldBe true

        val listTypeName =
          ParameterizedTypeName.get(classOf[List[_]], classOf[String])
        typesUtil.getDataTreeType(listTypeName).isDefined shouldBe true
        typesUtil.getDataTreeType(TypeName.VOID).isEmpty shouldBe true

        // 7. Subtype checks
        val listMirror = processingEnvironment.getElementUtils
          .getTypeElement("java.util.List")
          .asType()
        val arrayListMirror = processingEnvironment.getElementUtils
          .getTypeElement("java.util.ArrayList")
          .asType()
        val setMirror = processingEnvironment.getElementUtils
          .getTypeElement("java.util.Set")
          .asType()
        val mapMirror = processingEnvironment.getElementUtils
          .getTypeElement("java.util.Map")
          .asType()

        assertThat(typesUtil.isList(listMirror)).isTrue()
        assertThat(typesUtil.isList(arrayListMirror)).isTrue()
        assertThat(typesUtil.isSet(setMirror)).isTrue()
        assertThat(typesUtil.isMap(mapMirror)).isTrue()
        assertThat(typesUtil.isCollection(listMirror)).isTrue()

        // Edge cases in isSubtypeOf
        assertThat(typesUtil.isList(intType)).isFalse()
      })
      .thenExpectThat()
      .compilationSucceeds()
      .executeTest()
  }

  test("testConfigTypeExceptionOnErrorType") {
    Cute
      .unitTest()
      .when()
      .passInElement()
      .fromSourceString[TypeElement](
        "me.bristermitten.mittenlib.tests.TestConfigClass",
        """
          |package me.bristermitten.mittenlib.tests;
          |import io.toolisticon.cute.PassIn;
          |@PassIn
          |public class TestConfigClass {}
          |""".stripMargin
      )
      .intoUnitTest((processingEnvironment, element) => {
        ToolingProvider.setTooling(processingEnvironment)
        val typesUtil = new TypesUtil(
          processingEnvironment.getTypeUtils,
          processingEnvironment.getElementUtils,
          new GeneratedTypeCache()
        )

        // Create a mocked TypeMirror returning TypeKind.ERROR
        val mockErrorMirror = mock(classOf[TypeMirror])
        when(mockErrorMirror.getKind).thenReturn(TypeKind.ERROR)

        intercept[DTOReferenceException] {
          typesUtil.isConfigType(mockErrorMirror)
        }
      })
      .thenExpectThat()
      .compilationSucceeds()
      .executeTest()
  }
}
