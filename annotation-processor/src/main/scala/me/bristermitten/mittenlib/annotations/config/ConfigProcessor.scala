package me.bristermitten.mittenlib.annotations.config

import com.google.auto.service.AutoService
import com.google.inject.Guice
import com.palantir.javapoet.JavaFile
import io.toolisticon.aptk.common.ToolingProvider
import io.toolisticon.aptk.tools.AbstractAnnotationProcessor
import io.toolisticon.aptk.tools.MessagerUtils
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.ElementKind
import javax.lang.model.element.NestingKind
import javax.lang.model.element.TypeElement
import me.bristermitten.mittenlib.annotations.compile.ConfigImplGenerator
import me.bristermitten.mittenlib.annotations.compile.ConfigLoaderGenerator
import me.bristermitten.mittenlib.annotations.compile.ConfigLoaderModuleGenerator
import me.bristermitten.mittenlib.annotations.compile.ConfigProcessorModule
import me.bristermitten.mittenlib.annotations.compile.ConfigSaverGenerator
import me.bristermitten.mittenlib.annotations.compile.ConfigValidatorGenerator
import me.bristermitten.mittenlib.annotations.compile.ConfigurationClassNameGenerator
import me.bristermitten.mittenlib.annotations.compile.NewtypeImplGenerator
import me.bristermitten.mittenlib.annotations.domain.ConfigStructure
import me.bristermitten.mittenlib.annotations.exception.ConfigProcessingException
import me.bristermitten.mittenlib.annotations.util.NewtypeUtil
import me.bristermitten.mittenlib.annotations.parser.ConfigParser
import me.bristermitten.mittenlib.annotations.parser.CustomDeserializers
import me.bristermitten.mittenlib.annotations.parser.CustomSerializers
import me.bristermitten.mittenlib.annotations.parser.ParserError
import me.bristermitten.mittenlib.config.Config
import me.bristermitten.mittenlib.config.Newtype
import me.bristermitten.mittenlib.config.extension.CustomDeserializerFor
import me.bristermitten.mittenlib.config.extension.CustomSerializerFor
import scala.jdk.CollectionConverters.*

import cats.data.ValidatedNel
import cats.implicits.*

/** Annotation processor for generating configuration classes from DTO classes
  * marked with {@link Config}.
  */
@SupportedAnnotationTypes(
  Array(
    "me.bristermitten.mittenlib.config.Config",
    "me.bristermitten.mittenlib.config.Newtype"
  )
)
@SupportedSourceVersion(SourceVersion.RELEASE_21)
@AutoService(Array(classOf[Processor]))
class ConfigProcessor extends AbstractAnnotationProcessor:

  override def processAnnotations(
      annotations: java.util.Set[? <: TypeElement],
      roundEnv: RoundEnvironment
  ): Boolean =
    ToolingProvider.setTooling(processingEnv)
    val injector = Guice.createInjector(ConfigProcessorModule(processingEnv))

    // 1. Setup side-effecting registrations
    val customDeserializers = injector.getInstance(classOf[CustomDeserializers])
    roundEnv
      .getElementsAnnotatedWith(classOf[CustomDeserializerFor])
      .asScala
      .collect { case t: TypeElement => t }
      .foreach(customDeserializers.registerCustomDeserializer)

    val customSerializers = injector.getInstance(classOf[CustomSerializers])
    roundEnv
      .getElementsAnnotatedWith(classOf[CustomSerializerFor])
      .asScala
      .collect { case t: TypeElement => t }
      .foreach(customSerializers.registerCustomSerializer)

    val types = roundEnv
      .getElementsAnnotatedWith(classOf[Config])
      .asScala
      .collect { case t: TypeElement => t }
      .filter(_.getNestingKind == NestingKind.TOP_LEVEL)
      .toList

    val newtypes = roundEnv
      .getElementsAnnotatedWith(classOf[Newtype])
      .asScala
      .collect { case t: TypeElement => t }
      .toList

    val configParser = injector.getInstance(classOf[ConfigParser])

    // 2. Functional compilation/parsing pipeline using Cats ValidatedNel
    val newtypeResults: ValidatedNel[ParserError, List[TypeElement]] =
      newtypes.traverse(validateNewtype)

    val configResults: ValidatedNel[ParserError, List[ConfigStructure]] =
      types.traverse(configParser.parseAbstract)

    (newtypeResults, configResults).mapN((_, _)) match
      case cats.data.Validated.Invalid(errors) =>
        errors.toList.foreach(error =>
          MessagerUtils.error(error.element, error.message)
        )
        true

      case cats.data.Validated.Valid((validNewtypes, validConfigs)) =>
        // 3. Code generation
        val newtypeGenerator = NewtypeImplGenerator()
        val generator = injector.getInstance(classOf[ConfigImplGenerator])
        val loaderGenerator =
          injector.getInstance(classOf[ConfigLoaderGenerator])
        val saverGenerator = injector.getInstance(classOf[ConfigSaverGenerator])
        val validatorGenerator =
          injector.getInstance(classOf[ConfigValidatorGenerator])
        val moduleGenerator =
          injector.getInstance(classOf[ConfigLoaderModuleGenerator])
        val classNameGenerator =
          injector.getInstance(classOf[ConfigurationClassNameGenerator])

        val filesToWrite = List.newBuilder[JavaFile]

        // Emit interfaces implementation newtype classes
        validNewtypes
          .filter(_.getKind == ElementKind.INTERFACE)
          .foreach(nt => filesToWrite += newtypeGenerator.emit(nt))

        // Emit config implementation, loaders, savers, and validators
        validConfigs.foreach { ast =>
          filesToWrite += generator.emit(ast)
          filesToWrite += loaderGenerator.emit(ast)
          filesToWrite += saverGenerator.emit(ast)
          filesToWrite += validatorGenerator.emit(ast)
        }

        // Emit loader module
        if (validConfigs.nonEmpty) {
          val sortedConfigs = validConfigs.sortBy { ast =>
            val pubClass = classNameGenerator.getPublicClassName(ast)
            (pubClass.packageName(), pubClass.simpleName())
          }

          val rootPackage = sortedConfigs
            .map(ast =>
              classNameGenerator.getPublicClassName(ast).packageName()
            )
            .minBy(_.length)

          filesToWrite += moduleGenerator.emit(
            sortedConfigs.asJava,
            rootPackage
          )
        }

        // 4. Write files to Filer
        try filesToWrite.result().foreach(_.writeTo(processingEnv.getFiler))
        catch
          case e: Exception =>
            throw ConfigProcessingException(
              "Could not write generated config files",
              e
            )
        true

  private def validateNewtype(
      newtype: TypeElement
  ): ValidatedNel[ParserError, TypeElement] =
    val errors = List.newBuilder[ParserError]
    if (
      newtype.getKind != ElementKind.RECORD && newtype.getKind != ElementKind.INTERFACE
    ) {
      errors += ParserError(
        newtype,
        s"Newtype annotation only supports records and interfaces: ${newtype.getQualifiedName}"
      )
    } else if (newtype.getKind == ElementKind.RECORD) {
      if (newtype.getRecordComponents.size() != 1) {
        errors += ParserError(
          newtype,
          s"Newtype record ${newtype.getQualifiedName} must have exactly one component"
        )
      }
    } else if (newtype.getKind == ElementKind.INTERFACE) {
      val methods = NewtypeUtil.getAbstractMethods(newtype)
      if (methods.size != 1) {
        errors += ParserError(
          newtype,
          s"Newtype interface ${newtype.getQualifiedName} must have exactly one abstract method"
        )
      }
    }

    val errList = errors.result()
    if (errList.isEmpty) {
      newtype.validNel
    } else {
      cats.data.Validated.invalid(
        cats.data.NonEmptyList.fromListUnsafe(errList)
      )
    }
