package me.bristermitten.mittenlib.codegen

import com.google.auto.service.AutoService
import com.palantir.javapoet.ClassName
import com.palantir.javapoet.TypeName
import io.toolisticon.aptk.compilermessage.api.DeclareCompilerMessage
import io.toolisticon.aptk.tools.AbstractAnnotationProcessor
import io.toolisticon.aptk.tools.MessagerUtils
import io.toolisticon.aptk.tools.TypeUtils
import io.toolisticon.aptk.tools.corematcher.AptkCoreMatchers
import io.toolisticon.aptk.tools.wrapper.ElementWrapper
import io.toolisticon.aptk.tools.wrapper.TypeElementWrapper
import java.io.IOException
import java.util.*
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedOptions
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import me.bristermitten.mittenlib.codegen.record.RecordConstructorSpec
import me.bristermitten.mittenlib.codegen.record.RecordGenerator
import me.bristermitten.mittenlib.codegen.union.UnionGenerator
import scala.jdk.CollectionConverters.*

@SupportedSourceVersion(SourceVersion.RELEASE_21)
@AutoService(Array(classOf[Processor]))
@SupportedOptions(Array("org.gradle.annotation.processing.isolating"))
class MittenLibCodegenProcessor extends AbstractAnnotationProcessor {

  private def parseRecord(typeElementWrapper: TypeElementWrapper): Optional[RecordConstructorSpec] = {
    val getters = typeElementWrapper
      .filterEnclosedElements()
      .applyFilter(AptkCoreMatchers.IS_METHOD)
      .applyFilter(AptkCoreMatchers.HAS_NO_PARAMETERS)
      .getResult()

    val fields = getters.stream()
      .map(method => RecordConstructorSpec.RecordFieldSpec(
        method.getSimpleName().toString(), TypeName.get(method.getReturnType())))
      .toList()

    Optional.of(RecordConstructorSpec(
      "create", // TODO: make customisable
      fields
    ))
  }

  private def parseConstructor(
    method: ExecutableElement,
    typeElement: TypeElementWrapper,
    existingConstructors: java.util.Collection[String]
  ): Optional[RecordConstructorSpec] = {
    if (!TypeUtils.TypeComparison.isTypeEqual(
        method.getReturnType(), typeElement.asType().unwrap())) {
      MessagerUtils.error(
        method,
        MittenLibCodegenProcessorCompilerMessages.METHOD_BAD_RETURN,
        typeElement.unwrap()
      )
      return Optional.empty()
    }
    val constructorName = method.getSimpleName().toString()
    if (existingConstructors.stream().anyMatch(con => con.equals(constructorName))) {
      MessagerUtils.error(
        method,
        MittenLibCodegenProcessorCompilerMessages.DUPLICATE_CONSTRUCTOR,
        constructorName
      )
      return Optional.empty()
    }
    Optional.of(RecordConstructorSpec(
      constructorName,
      method.getParameters().stream()
        .map(param => RecordConstructorSpec.RecordFieldSpec(
          param.getSimpleName().toString(), TypeName.get(param.asType())))
        .toList()
    ))
  }

  private def getSpecName(spec: TypeElement): ClassName = {
    val wrapped = TypeElementWrapper.wrap(spec)
    val explicitName = wrapped.getAnnotation(classOf[RecordSpec])
      .map(anno => anno.name())
      .or(() => wrapped.getAnnotation(classOf[UnionSpec]).map(anno => anno.name()))
      .filter(name => !name.isBlank())

    val recordSpecName = ClassName.get(spec)
    if (explicitName.isPresent()) {
      ClassName.get(recordSpecName.packageName(), explicitName.get())
    } else {
      ClassName.get(
        recordSpecName.packageName(),
        recordSpecName.simpleName().replace("Spec", "")
      )
    }
  }

  override def getSupportedAnnotationTypes(): java.util.Set[String] = {
    AbstractAnnotationProcessor.createSupportedAnnotationSet(classOf[RecordSpec], classOf[UnionSpec])
  }

  @DeclareCompilerMessage(
    code = "001",
    enumValueName = "METHOD_BAD_RETURN",
    message = "Method must return the record type ${0}!"
  )
  @DeclareCompilerMessage(
    code = "002",
    enumValueName = "DUPLICATE_CONSTRUCTOR",
    message = "Constructors must have distinct names, overloading is not allowed"
  )
  override def processAnnotations(annotations: java.util.Set[? <: TypeElement], roundEnv: RoundEnvironment): Boolean = {
    val unions = processUnions(roundEnv)
    val records = processRecords(roundEnv)
    unions && records
  }

  @DeclareCompilerMessage(code = "003", enumValueName = "INVALID_RECORD", message = "Could not parse record ${0}.")
  private def processRecords(roundEnv: RoundEnvironment): Boolean = {
    val records = new java.util.ArrayList[me.bristermitten.mittenlib.codegen.record.RecordSpec]()
    var hasError = false

    val elements = roundEnv.getElementsAnnotatedWith(classOf[RecordSpec]).asScala
    val iterator = elements.iterator
    while (iterator.hasNext && !hasError) {
      val element = iterator.next()
      val wrap = ElementWrapper.wrap(element)
      wrap.validateWithFluentElementValidator()
        .is(AptkCoreMatchers.IS_INTERFACE)
        .validateAndIssueMessages()

      val typeElement = ElementWrapper.toTypeElement(wrap)

      val recordConstructorSpec = parseRecord(typeElement)
      if (recordConstructorSpec.isEmpty()) {
        MessagerUtils.error(element, MittenLibCodegenProcessorCompilerMessages.INVALID_RECORD, typeElement)
        hasError = true
      } else {
        val constructor = recordConstructorSpec.get()
        val recordSpecName = getSpecName(typeElement.unwrap())
        val recordSpec = me.bristermitten.mittenlib.codegen.record.RecordSpec(
          ClassName.get(typeElement.unwrap()), recordSpecName, constructor)
        records.add(recordSpec)
      }
    }

    if (hasError) {
      return false
    }

    val generator = new RecordGenerator()
    for (record <- records.asScala) {
      val generate = generator.generate(record)
      try {
        generate.writeTo(processingEnv.getFiler())
      } catch {
        case e: IOException => throw new RuntimeException(e)
      }
    }
    true
  }

  private def processUnions(roundEnv: RoundEnvironment): Boolean = {
    val unions = new java.util.ArrayList[me.bristermitten.mittenlib.codegen.union.UnionSpec]()
    for (element <- roundEnv.getElementsAnnotatedWith(classOf[UnionSpec]).asScala) {
      val wrap = ElementWrapper.wrap(element)
      wrap.validateWithFluentElementValidator()
        .is(AptkCoreMatchers.IS_INTERFACE)
        .validateAndIssueMessages()

      val typeElement = ElementWrapper.toTypeElement(wrap)
      val constructors = new java.util.ArrayList[RecordConstructorSpec]()

      val enclosed = typeElement.filterEnclosedElements()
        .applyFilter(AptkCoreMatchers.IS_METHOD)
        .getResult()

      for (method <- enclosed.asScala) {
        val existingNames = constructors.stream().map(con => con.name).toList()
        val recordConstructorSpec = parseConstructor(method, typeElement, existingNames)
        if (recordConstructorSpec.isPresent()) {
          constructors.add(recordConstructorSpec.get())
        }
      }

      val recordSpecName = getSpecName(typeElement.unwrap())

      val matchStrategy = typeElement
        .getAnnotation(classOf[MatchStrategy])
        .map(anno => anno.value())
        .orElse(MatchStrategies.NOMINAL)

      val recordSpec = me.bristermitten.mittenlib.codegen.union.UnionSpec(
        ClassName.get(typeElement.unwrap()), recordSpecName, matchStrategy, constructors)
      unions.add(recordSpec)
    }

    if (unions.isEmpty()) {
      return false
    }

    val generator = new UnionGenerator()
    for (union <- unions.asScala) {
      val generate = generator.generate(union)
      try {
        generate.writeTo(processingEnv.getFiler())
      } catch {
        case e: IOException => throw new RuntimeException(e)
      }
    }
    true
  }
}
