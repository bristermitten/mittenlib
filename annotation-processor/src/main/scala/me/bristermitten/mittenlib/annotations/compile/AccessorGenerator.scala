package me.bristermitten.mittenlib.annotations.compile

import com.google.inject.Inject
import com.palantir.javapoet.*
import io.toolisticon.aptk.tools.wrapper.AnnotationMirrorWrapper
import java.util.StringJoiner
import javax.lang.model.element.{AnnotationMirror, ExecutableElement, Modifier, TypeElement, VariableElement}
import me.bristermitten.mittenlib.annotations.ast.{AbstractConfigStructure, ConfigTypeSource, Property}
import me.bristermitten.mittenlib.annotations.util.{PrivateAnnotations, TypeSpecUtil}
import me.bristermitten.mittenlib.util.Strings
import org.jetbrains.annotations.Contract
import scala.jdk.CollectionConverters.*

/**
 * Generates accessor methods for configuration classes. This class creates getter methods for
 * fields and "with" methods that act as immutable setters, returning new instances with modified
 * values. It also handles method overriding and annotation preservation when generating accessor
 * methods.
 */
class AccessorGenerator @Inject() (
  private val methodNames: MethodNames,
  private val configurationClassNameGenerator: ConfigurationClassNameGenerator
):

  /**
   * Creates a getter method for a field.
   *
   * @param typeSpecBuilder The builder for the type spec
   * @param element         The variable element
   * @param field           The field spec
   */
  def createGetterMethod(typeSpecBuilder: TypeSpec.Builder, element: VariableElement, field: FieldSpec): Unit =
    val safeName = getFieldAccessorName(element)

    val builder = MethodSpec.methodBuilder(safeName)
      .addJavadoc(
        """Gets the value of the {@code $L} property.
          |
          |@return the value of the property
          |""".stripMargin, field.name)
      .addModifiers(Modifier.PUBLIC)
      .returns(field.`type`)
      .addStatement("return " + field.name)

    builder.addAnnotation(AnnotationSpec.builder(classOf[Contract])
      .addMember("pure", CodeBlock.of("true"))
      .build())
    typeSpecBuilder.addMethod(builder.build())

  /**
   * Creates a getter method that overrides an existing method. This method preserves annotations
   * from the original method (except for private annotations) and adds appropriate nullability and
   * contract annotations.
   *
   * @param typeSpecBuilder The builder for the type spec
   * @param overriding      The executable element being overridden
   * @param fromField       The field spec that the getter will return
   */
  def createGetterMethodOverriding(
    typeSpecBuilder: TypeSpec.Builder,
    overriding: ExecutableElement,
    fromField: FieldSpec
  ): Unit =
    val builder = MethodSpec.methodBuilder(overriding.getSimpleName.toString)
      .addJavadoc(
        """Gets the value of the {@code $L} property, overriding the original config method.
          |
          |@return the value of the property
          |""".stripMargin, fromField.name)
      .addModifiers(Modifier.PUBLIC)
      .returns(fromField.`type`)
      .addStatement("return " + fromField.name)
      .addAnnotation(classOf[Override])

    for (annotationMirror <- overriding.getAnnotationMirrors.asScala) {
      if (!PrivateAnnotations.isPrivate(
          AnnotationMirrorWrapper.wrap(annotationMirror).asElement().getQualifiedName)) {
        builder.addAnnotation(AnnotationSpec.get(annotationMirror))
      }
    }

    TypeSpecUtil.methodAddAnnotation(builder, classOf[Contract], b => b.addMember("pure", CodeBlock.of("true")))
    typeSpecBuilder.addMethod(builder.build())

  /**
   * Creates "with" methods (immutable setters) for each field.
   *
   * @param typeSpecBuilder The builder for the type spec
   * @param ast             The config ast
   */
  def createWithMethods(typeSpecBuilder: TypeSpec.Builder, ast: AbstractConfigStructure): Unit =
    for (field <- ast.properties().asScala) {
      val configImplClassName = configurationClassNameGenerator.generateConfigurationClassName(ast.source().element())
      val withMethodBuilder = MethodSpec.methodBuilder("with" + Strings.capitalize(field.name()))
        .addJavadoc(
          """Returns a new instance of this configuration with the {@code $L} property updated.
            |
            |@param $L the new value for the property
            |@return a new configuration instance with the updated value
            |""".stripMargin, field.name(), field.name())
        .addModifiers(Modifier.PUBLIC)
        .returns(configImplClassName)
        .addParameter(ParameterSpec.builder(
            configurationClassNameGenerator.publicPropertyClassName(field), field.name())
          .addModifiers(Modifier.FINAL)
          .build())

      ast match {
        case _: AbstractConfigStructure.Union =>
          // make the with method abstract and then alternatives can override it
          withMethodBuilder.addModifiers(Modifier.ABSTRACT)
          typeSpecBuilder.addMethod(withMethodBuilder.build())
        case _ =>
          // Create a string representing the constructor parameters
          var constructorParams = Strings.joinWith(
            ast.properties(),
            (f2: Property) => {
              if (f2.name() == field.name()) {
                f2.name()
              } else {
                "this." + f2.name()
              }
            },
            ", "
          )

          ast.source() match {
            case classSource: ConfigTypeSource.ClassConfigTypeSource if classSource.parent().isPresent =>
              val joiner = new StringJoiner(", ").add("this.parent")
              if (!constructorParams.isEmpty) {
                joiner.add(constructorParams)
              }
              constructorParams = joiner.toString
            case _ =>
          }

          typeSpecBuilder.addMethod(withMethodBuilder
            .addStatement("return new $T(" + constructorParams + ")", configImplClassName)
            .build())
      }
    }

  /**
   * Gets the accessor name for a field.
   *
   * @param variableElement The variable element
   * @return The accessor name
   */
  private def getFieldAccessorName(variableElement: VariableElement): String =
    methodNames.safeMethodName(variableElement, variableElement.getEnclosingElement.asInstanceOf[TypeElement])
