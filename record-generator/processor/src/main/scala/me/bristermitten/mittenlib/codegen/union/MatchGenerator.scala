package me.bristermitten.mittenlib.codegen.union

import com.palantir.javapoet.*
import java.util.function.BiConsumer
import java.util.function.BiFunction
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.IntConsumer
import java.util.function.IntFunction
import java.util.function.Supplier
import javax.lang.model.element.Modifier
import me.bristermitten.mittenlib.codegen.MatchStrategies
import me.bristermitten.mittenlib.codegen.record.RecordConstructorSpec
import scala.jdk.CollectionConverters.*

object MatchGenerator:

  def makeVoidMatchMethodSpec(spec: ResolvedUnionSpec, makeAbstract: Boolean): MethodSpec =
    val builder = MethodSpec.methodBuilder("match")
      .addModifiers(Modifier.PUBLIC)
      .returns(TypeName.VOID)
      .addParameters(
        spec.constructors.asScala.map { constructor =>
          ParameterSpec.builder(
            voidFunctionalInterfaceFor(constructor, spec.strategy),
            constructor.name.simpleName()
          ).build()
        }.asJava
      )

    if (makeAbstract) {
      builder.addModifiers(Modifier.ABSTRACT)
    }
    builder.build()

  def makeMatchMethodSpec(spec: ResolvedUnionSpec, makeAbstract: Boolean): MethodSpec =
    val builder = MethodSpec.methodBuilder("matchTo")
      .addModifiers(Modifier.PUBLIC)
      .addTypeVariable(TypeVariableName.get("T"))
      .returns(TypeVariableName.get("T"))
      .addParameters(
        spec.constructors.asScala.map { constructor =>
          ParameterSpec.builder(
            returningFunctionalInterfaceFor(constructor, spec.strategy, TypeVariableName.get("T")),
            constructor.name.simpleName()
          ).build()
        }.asJava
      )
    if (makeAbstract) {
      builder.addModifiers(Modifier.ABSTRACT)
    }
    builder.build()

  def implementVoidMatchMethod(record: ResolvedUnionSpec, spec: ResolvedUnionConstructor): MethodSpec =
    val usedFunctionalInterface = voidFunctionalInterfaceFor(spec, record.strategy)
    val functionalInterfaceInvokeName = this.functionalInterfaceInvokeName(usedFunctionalInterface)
    val m = makeVoidMatchMethodSpec(record, false).toBuilder()
      .addAnnotation(classOf[Override])
      .addCode(
        CodeBlock.builder()
          .add("$L.$L", spec.name.simpleName(), functionalInterfaceInvokeName)
          .addStatement(matchParameters(record.strategy, spec.constructor))
          .build()
      )
    m.build()

  private def matchParameters(strategy: MatchStrategies, constructor: RecordConstructorSpec): CodeBlock =
    strategy match {
      case MatchStrategies.NOMINAL =>
        CodeBlock.of("(this)")
      case MatchStrategies.STRUCTURAL =>
        constructor.fields.asScala
          .map(field => CodeBlock.of("this.$L", field.name))
          .asJava.stream().collect(CodeBlock.joining(", ", "(", ")"))
    }

  def implementReturningMatchMethod(record: ResolvedUnionSpec, spec: ResolvedUnionConstructor): MethodSpec =
    val usedFunctionalInterface = returningFunctionalInterfaceFor(spec, record.strategy, TypeVariableName.get("T"))
    val invokeName = functionalInterfaceInvokeName(usedFunctionalInterface)
    makeMatchMethodSpec(record, false).toBuilder()
      .addAnnotation(classOf[Override])
      .returns(TypeVariableName.get("T"))
      .addCode(
        CodeBlock.builder()
          .add("return $L.$L", spec.name.simpleName(), invokeName)
          .addStatement(matchParameters(record.strategy, spec.constructor))
          .build()
      )
      .build()

  def voidFunctionalInterfaceFor(
    constructor: ResolvedUnionConstructor,
    strategies: MatchStrategies
  ): TypeName =
    if (strategies == MatchStrategies.NOMINAL) {
      return ParameterizedTypeName.get(ClassName.get(classOf[Consumer[?]]), constructor.name)
    }
    val fields = constructor.constructor.fields.asScala
    fields.size match {
      case 0 => ClassName.get(classOf[Runnable])
      case 1 =>
        val firstField = fields.head
        val tpe = firstField.`type`
        if (tpe == TypeName.INT) {
          ClassName.get(classOf[IntConsumer])
        } else {
          ParameterizedTypeName.get(ClassName.get(classOf[Consumer[?]]), tpe.box())
        }
      case 2 =>
        ParameterizedTypeName.get(
          ClassName.get(classOf[BiConsumer[?, ?]]),
          fields(0).`type`.box(),
          fields(1).`type`.box()
        )
      case _ =>
        throw new UnsupportedOperationException("Unsupported number of fields for match method: " + fields.size)
    }

  def returningFunctionalInterfaceFor(
    spec: ResolvedUnionConstructor,
    strategies: MatchStrategies,
    returning: TypeName
  ): TypeName =
    strategies match {
      case MatchStrategies.NOMINAL =>
        ParameterizedTypeName.get(ClassName.get(classOf[Function[?, ?]]), spec.name, returning)
      case MatchStrategies.STRUCTURAL =>
        val fields = spec.constructor.fields.asScala
        fields.size match {
          case 0 =>
            ParameterizedTypeName.get(ClassName.get(classOf[Supplier[?]]), returning)
          case 1 =>
            val firstField = fields.head
            val tpe = firstField.`type`
            if (tpe == TypeName.INT) {
              ParameterizedTypeName.get(ClassName.get(classOf[IntFunction[?]]), returning)
            } else {
              ParameterizedTypeName.get(ClassName.get(classOf[Function[?, ?]]), tpe.box(), returning)
            }
          case 2 =>
            ParameterizedTypeName.get(
              ClassName.get(classOf[BiFunction[?, ?, ?]]),
              fields(0).`type`.box(),
              fields(1).`type`.box(),
              returning
            )
          case _ =>
            throw new UnsupportedOperationException("Unsupported number of fields for match method: " + fields.size)
        }
    }

  private def functionalInterfaceInvokeName(fi: TypeName): String =
    fi match {
      case c: ClassName if c.equals(ClassName.get(classOf[Runnable])) => "run"
      case c: ClassName if c.equals(ClassName.get(classOf[IntConsumer])) => "accept"
      case p: ParameterizedTypeName =>
        val raw = p.rawType()
        if (raw.equals(ClassName.get(classOf[Consumer[?]]))) "accept"
        else if (raw.equals(ClassName.get(classOf[Function[?, ?]]))) "apply"
        else if (raw.equals(ClassName.get(classOf[IntFunction[?]]))) "apply"
        else if (raw.equals(ClassName.get(classOf[Supplier[?]]))) "get"
        else if (raw.equals(ClassName.get(classOf[BiConsumer[?, ?]]))) "accept"
        else if (raw.equals(ClassName.get(classOf[BiFunction[?, ?, ?]]))) "apply"
        else throw new UnsupportedOperationException("Unsupported functional interface: " + fi)
      case _ =>
        throw new UnsupportedOperationException("Unsupported functional interface: " + fi)
    }
