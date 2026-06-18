package me.bristermitten.mittenlib.annotations.compile

import com.palantir.javapoet.*
import java.util.Objects
import javax.annotation.processing.Generated
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.util.ElementFilter
import me.bristermitten.mittenlib.annotations.config.ConfigProcessor
import me.bristermitten.mittenlib.annotations.util.NewtypeUtil
import scala.jdk.CollectionConverters.*

class NewtypeImplGenerator:

  def emit(element: TypeElement): JavaFile =
    val publicClassName = ClassName.get(element)
    val implClassName = NewtypeUtil.getImplClassName(element)

    val builder = TypeSpec.classBuilder(implClassName)
      .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
      .addAnnotation(GeneratorUtil.generatedAnnotation())

    // Handle generic type parameters
    val typeParams = element.getTypeParameters.asScala
    if (typeParams.nonEmpty) {
      for (tp <- typeParams) {
        builder.addTypeVariable(TypeVariableName.get(tp))
      }
      val typeVars = typeParams.map(TypeVariableName.get).toArray
      builder.addSuperinterface(ParameterizedTypeName.get(publicClassName, typeVars *))
    } else {
      builder.addSuperinterface(publicClassName)
    }

    val method = ElementFilter.methodsIn(element.getEnclosedElements).asScala
      .find(m => !m.isDefault && !m.getModifiers.contains(Modifier.STATIC))
      .getOrElse(throw new IllegalArgumentException("No abstract method found in newtype interface"))

    val fieldName = method.getSimpleName.toString
    val typeName = TypeName.get(method.getReturnType)

    // Field
    builder.addField(
      FieldSpec.builder(typeName, fieldName, Modifier.PRIVATE, Modifier.FINAL).build()
    )

    // Constructor
    builder.addMethod(
      MethodSpec.constructorBuilder()
        .addModifiers(Modifier.PUBLIC)
        .addParameter(typeName, fieldName)
        .addStatement("this.$1L = $1L", fieldName)
        .build()
    )

    // Getter
    builder.addMethod(
      MethodSpec.methodBuilder(fieldName)
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(classOf[Override])
        .returns(typeName)
        .addStatement("return this.$L", fieldName)
        .build()
    )

    // equals
    builder.addMethod(
      MethodSpec.methodBuilder("equals")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(classOf[Override])
        .returns(classOf[Boolean])
        .addParameter(classOf[Any], "o")
        .addStatement("if (this == o) return true")
        .addStatement("if (!(o instanceof $T)) return false", publicClassName)
        .addStatement("$T other = ($T) o", publicClassName, publicClassName)
        .addStatement("return $T.equals(this.$L, other.$L())", classOf[Objects], fieldName, fieldName)
        .build()
    )

    // hashCode
    builder.addMethod(
      MethodSpec.methodBuilder("hashCode")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(classOf[Override])
        .returns(classOf[Int])
        .addStatement("return $T.hash(this.$L)", classOf[Objects], fieldName)
        .build()
    )

    // toString
    builder.addMethod(
      MethodSpec.methodBuilder("toString")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(classOf[Override])
        .returns(classOf[String])
        .addStatement("return $S + $L + $S", implClassName.simpleName() + "{" + fieldName + "=", fieldName, "}")
        .build()
    )

    JavaFile.builder(implClassName.packageName(), builder.build())
      .skipJavaLangImports(true)
      .build()
