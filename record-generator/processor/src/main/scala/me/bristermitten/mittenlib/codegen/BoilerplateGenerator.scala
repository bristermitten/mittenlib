package me.bristermitten.mittenlib.codegen

import com.palantir.javapoet.ArrayTypeName
import com.palantir.javapoet.ClassName
import com.palantir.javapoet.MethodSpec
import java.util.Arrays
import java.util.Objects
import javax.lang.model.element.Modifier
import me.bristermitten.mittenlib.codegen.record.RecordConstructorSpec
import scala.jdk.CollectionConverters.*

import _root_.me.bristermitten.mittenlib.codegen.dsl.*
import _root_.me.bristermitten.mittenlib.codegen.dsl.BlockBuilder.*
import _root_.me.bristermitten.mittenlib.codegen.dsl.given

object BoilerplateGenerator:

  private def toSharedFields(
      recordConstructorSpec: RecordConstructorSpec
  ): List[SharedField] =
    recordConstructorSpec.fields.asScala.toList.map { f =>
      val t = TypeRef.of(f.`type`)
      val isArr = f.`type`.isInstanceOf[com.palantir.javapoet.ArrayTypeName]
      SharedField(
        name = f.name,
        tpe = t,
        accessor = receiver => receiver.field(f.name),
        isArray = isArr
      )
    }

  def genEquals(
      recordConstructorSpec: RecordConstructorSpec,
      name: ClassName
  ): MethodSpec =
    val methodDecl =
      BoilerplateHelper.equalsDecl(name, toSharedFields(recordConstructorSpec))
    CodeBlockRenderer.renderMethod(methodDecl)

  def genHashCode(recordConstructorSpec: RecordConstructorSpec): MethodSpec =
    val methodDecl =
      BoilerplateHelper.hashCodeDecl(toSharedFields(recordConstructorSpec))
    CodeBlockRenderer.renderMethod(methodDecl)

  def genToString(
      recordConstructorSpec: RecordConstructorSpec,
      name: ClassName
  ): MethodSpec =
    val methodDecl = BoilerplateHelper.toStringDecl(
      name,
      toSharedFields(recordConstructorSpec),
      ", "
    )
    CodeBlockRenderer.renderMethod(methodDecl)
