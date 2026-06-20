package me.bristermitten.mittenlib.codegen

import com.palantir.javapoet.{ClassName, TypeName}
import io.toolisticon.aptk.tools.{
  AbstractAnnotationProcessor,
  MessagerUtils,
  TypeUtils
}
import io.toolisticon.aptk.tools.corematcher.AptkCoreMatchers
import io.toolisticon.aptk.tools.wrapper.{ElementWrapper, TypeElementWrapper}
import me.bristermitten.mittenlib.codegen.record.{
  RecordConstructorSpec,
  RecordGenerator
}
import me.bristermitten.mittenlib.codegen.union.UnionGenerator

import java.io.IOException
import java.util.Optional
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.{ExecutableElement, TypeElement}
import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

class MittenLibCodegenProcessor extends AbstractAnnotationProcessor {

  private def parseRecord(
      typeElementWrapper: TypeElementWrapper
  ): Option[RecordConstructorSpec] = {
    val getters = typeElementWrapper
      .filterEnclosedElements()
      .applyFilter(AptkCoreMatchers.IS_METHOD)
      .applyFilter(AptkCoreMatchers.HAS_NO_PARAMETERS)
      .getResult()

    val fields = getters
      .stream()
      .map(method =>
        RecordConstructorSpec.RecordFieldSpec(
          method.getSimpleName().toString(),
          TypeName.get(method.getReturnType())
        )
      )
      .toList()

    Some(
      RecordConstructorSpec(
        "create", // TODO: make customisable
        fields
      )
    )
  }

  private def parseConstructor(
      method: ExecutableElement,
      typeElement: TypeElementWrapper,
      existingConstructors: java.util.Collection[String]
  ): Option[RecordConstructorSpec] = {
    if (
      !TypeUtils.TypeComparison.isTypeEqual(
        method.getReturnType(),
        typeElement.asType().unwrap()
      )
    ) {
      MessagerUtils.error(
        method,
        MittenLibCodegenProcessorMessagesCompilerMessages.METHOD_BAD_RETURN,
        typeElement.unwrap()
      )
      return None
    }
    val constructorName = method.getSimpleName().toString()
    if (
      existingConstructors.stream().anyMatch(con => con.equals(constructorName))
    ) {
      MessagerUtils.error(
        method,
        MittenLibCodegenProcessorMessagesCompilerMessages.DUPLICATE_CONSTRUCTOR,
        constructorName
      )
      return None
    }
    Some(
      RecordConstructorSpec(
        constructorName,
        method
          .getParameters()
          .stream()
          .map(param =>
            RecordConstructorSpec.RecordFieldSpec(
              param.getSimpleName().toString(),
              TypeName.get(param.asType())
            )
          )
          .toList()
      )
    )
  }

  private def getSpecName(spec: TypeElement): ClassName = {
    val wrapped = TypeElementWrapper.wrap(spec)
    val explicitName = wrapped
      .getAnnotation(classOf[RecordSpec])
      .toScala
      .map(_.name())
      .orElse(wrapped.getAnnotation(classOf[UnionSpec]).toScala.map(_.name()))
      .filter(!_.isBlank)

    val recordSpecName = ClassName.get(spec)
    explicitName match {
      case Some(name) =>
        ClassName.get(recordSpecName.packageName(), name)
      case None =>
        ClassName.get(
          recordSpecName.packageName(),
          recordSpecName.simpleName().replace("Spec", "")
        )
    }
  }

  override def getSupportedAnnotationTypes(): java.util.Set[String] = {
    AbstractAnnotationProcessor.createSupportedAnnotationSet(
      classOf[RecordSpec],
      classOf[UnionSpec]
    )
  }

  override def processAnnotations(
      annotations: java.util.Set[? <: TypeElement],
      roundEnv: RoundEnvironment
  ): Boolean = {
    val unions = processUnions(roundEnv)
    val records = processRecords(roundEnv)
    unions && records
  }

  private def processRecords(roundEnv: RoundEnvironment): Boolean = {
    val elements =
      roundEnv.getElementsAnnotatedWith(classOf[RecordSpec]).asScala.toList

    val parseResults = elements.flatMap { element =>
      val typeElement = validateAndGetTypeElement(element)
      parseRecord(typeElement) match {
        case None =>
          MessagerUtils.error(
            element,
            MittenLibCodegenProcessorMessagesCompilerMessages.INVALID_RECORD,
            typeElement
          )
          None
        case Some(constructor) =>
          val recordSpecName = getSpecName(typeElement.unwrap())
          Some(
            me.bristermitten.mittenlib.codegen.record.RecordSpec(
              ClassName.get(typeElement.unwrap()),
              recordSpecName,
              constructor
            )
          )
      }
    }

    if (parseResults.size != elements.size) {
      return false
    }

    val generator = new RecordGenerator()
    parseResults.foreach { record =>
      try {
        generator.generate(record).writeTo(processingEnv.getFiler())
      } catch {
        case e: IOException => throw new RuntimeException(e)
      }
    }
    true
  }

  private def processUnions(roundEnv: RoundEnvironment): Boolean = {
    val elements =
      roundEnv.getElementsAnnotatedWith(classOf[UnionSpec]).asScala.toList
    if (elements.isEmpty) {
      return false
    }

    val unionSpecs = elements.flatMap { element =>
      val typeElement = validateAndGetTypeElement(element)
      val enclosed = typeElement
        .filterEnclosedElements()
        .applyFilter(AptkCoreMatchers.IS_METHOD)
        .getResult()

      val constructors =
        enclosed.asScala.foldLeft(List.empty[RecordConstructorSpec]) {
          (acc, method) =>
            val existingNames = acc.map(_.name).asJava
            parseConstructor(method, typeElement, existingNames) match {
              case Some(spec) => acc :+ spec
              case None       => acc
            }
        }

      val recordSpecName = getSpecName(typeElement.unwrap())
      val matchStrategy = typeElement
        .getAnnotation(classOf[MatchStrategy])
        .toScala
        .map(_.value())
        .getOrElse(MatchStrategies.NOMINAL)

      Some(
        me.bristermitten.mittenlib.codegen.union.UnionSpec(
          ClassName.get(typeElement.unwrap()),
          recordSpecName,
          matchStrategy,
          constructors.asJava
        )
      )
    }

    val generator = new UnionGenerator()
    unionSpecs.foreach { union =>
      try {
        generator.generate(union).writeTo(processingEnv.getFiler())
      } catch {
        case e: IOException => throw new RuntimeException(e)
      }
    }
    true
  }

  private def validateAndGetTypeElement(
      element: javax.lang.model.element.Element
  ): TypeElementWrapper =
    val wrap = ElementWrapper.wrap(element)
    wrap
      .validateWithFluentElementValidator()
      .is(AptkCoreMatchers.IS_INTERFACE)
      .validateAndIssueMessages()
    ElementWrapper.toTypeElement(wrap)
}
