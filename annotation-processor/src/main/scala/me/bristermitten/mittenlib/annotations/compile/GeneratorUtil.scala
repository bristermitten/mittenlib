package me.bristermitten.mittenlib.annotations.compile

import com.palantir.javapoet.{ClassName, CodeBlock, MethodSpec}
import me.bristermitten.mittenlib.annotations.ast.{AbstractConfigStructure, ConfigTypeSource, Property}
import org.jspecify.annotations.Nullable

object GeneratorUtil:

  def getDaoName(
    ast: AbstractConfigStructure,
    classNameGenerator: ConfigurationClassNameGenerator
  ): ClassName =
    ast.source() match {
      case _: ConfigTypeSource.InterfaceConfigTypeSource => classNameGenerator.getInnerDaoName(ast)
      case _: ConfigTypeSource.ClassConfigTypeSource => ast.name()
    }

  def addDaoInstantiationIfNecessary(
    ast: AbstractConfigStructure,
    methodBuilder: MethodSpec.Builder,
    @Nullable daoName: ClassName
  ): Unit =
    val hasAnyDefault = ast.properties().stream().anyMatch(p => p.settings().hasDefaultValue())
    if (daoName != null && hasAnyDefault) {
      methodBuilder.addStatement("$T dao = new $T()", daoName, daoName)
    }

  def getPropertyAccess(
    ast: AbstractConfigStructure,
    property: Property,
    configExpr: me.bristermitten.mittenlib.codegen.dsl.Expr,
    methodNames: MethodNames,
    useGetters: Boolean
  ): me.bristermitten.mittenlib.codegen.dsl.Expr =
    ast.source() match {
      case _: ConfigTypeSource.InterfaceConfigTypeSource =>
        configExpr.call(property.name())
      case _: ConfigTypeSource.ClassConfigTypeSource =>
        if (useGetters) {
          configExpr.call(methodNames.safeMethodName(property))
        } else {
          configExpr.field(property.name())
        }
    }
