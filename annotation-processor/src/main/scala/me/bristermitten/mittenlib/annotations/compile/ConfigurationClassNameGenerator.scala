package me.bristermitten.mittenlib.annotations.compile

import com.google.inject.Inject
import com.palantir.javapoet.ClassName
import com.palantir.javapoet.ParameterizedTypeName
import com.palantir.javapoet.TypeName
import io.toolisticon.aptk.tools.wrapper.TypeElementWrapper
import java.util.List
import javax.lang.model.element.NestingKind
import javax.lang.model.element.TypeElement
import javax.lang.model.`type`.DeclaredType
import javax.lang.model.`type`.TypeMirror
import me.bristermitten.mittenlib.annotations.domain.{
  ConfigStructure,
  Property => DomainProperty
}
import me.bristermitten.mittenlib.annotations.util.ConfigStructureAnalysis
import me.bristermitten.mittenlib.config.Config
import me.bristermitten.mittenlib.util.Strings
import org.jspecify.annotations.Nullable
import scala.jdk.CollectionConverters.*

object ConfigurationClassNameGenerator:
  val DESERIALIZER_SUFFIX = "Deserializer"
  val SERIALIZER_SUFFIX = "Serializer"
  val VALIDATOR_SUFFIX = "Validator"
  val PROVIDER_SUFFIX = "Provider"
  val DEFAULT_METHOD_ACCESS_SUFFIX = "DefaultMethodAccess"
  val CONFIG_LOADER_MODULE_NAME = "ConfigLoaderModule"

  def translateConfigClassName(dtoClassName: ClassName): ClassName =
    val implName = if (dtoClassName.simpleName().endsWith("DTO")) {
      dtoClassName
        .simpleName()
        .substring(0, dtoClassName.simpleName().length() - 3)
    } else {
      dtoClassName.simpleName() + "Impl"
    }
    dtoClassName.peerClass(implName)

  private def findConfigClassName(dtoType: TypeElement): String =
    val annotation = dtoType.getAnnotation(classOf[Config])
    if (annotation == null) {
      ClassName.get(dtoType).simpleName()
    } else if (annotation.className().nonEmpty) {
      annotation.className()
    } else {
      translateConfigClassName(ClassName.get(dtoType)).simpleName()
    }

  def getCleanSimpleName(name: ClassName): String =
    val simpleName = name.simpleName()
    if (simpleName.endsWith("DTO")) {
      simpleName.substring(0, simpleName.length() - 3)
    } else {
      simpleName
    }

class ConfigurationClassNameGenerator @Inject() (
    private val configNameCache: ConfigNameCache,
    private val configStructureAnalysis: ConfigStructureAnalysis
):
  import ConfigurationClassNameGenerator.*

  def translateConfigClassName(ast: ConfigStructure): ClassName =
    val manualName = ast.settings.className.filter(_.nonEmpty)
    val simpleName = ast.name.simpleName()
    val implSimpleName = manualName.getOrElse {
      if (simpleName.endsWith("DTO")) simpleName.dropRight(3)
      else simpleName + "Impl"
    }
    val enclosing = ast.name.enclosingClassName()
    if (enclosing != null) {
      configNameCache.lookupDomain(enclosing) match {
        case Some(enclosingAst) =>
          translateConfigClassName(enclosingAst).nestedClass(implSimpleName)
        case None =>
          ConfigurationClassNameGenerator.translateConfigClassName(ast.name)
      }
    } else {
      ast.name.peerClass(implSimpleName)
    }

  def translateConfigClassName(className: ClassName): ClassName =
    configNameCache.lookupDomain(className) match {
      case Some(ast) => translateConfigClassName(ast)
      case None      =>
        ConfigurationClassNameGenerator.translateConfigClassName(className)
    }

  def getPublicClassName(ast: ConfigStructure): ClassName =
    ast match {
      case a: ConfigStructure.Atomic if a.isInterface => a.name
      case _: ConfigStructure.Intersection            => ast.name
      case _: ConfigStructure.Union                   => ast.name
      case _ => translateConfigClassName(ast)
    }

  def getConcreteConfigClassName(ast: ConfigStructure): ClassName =
    ast match {
      case a: ConfigStructure.Atomic if !a.isInterface => a.name
      case _ => translateConfigClassName(ast)
    }

  private def translateDTOParameters(
      mirror: TypeMirror,
      getConfigClassName: TypeMirror => TypeName
  ): TypeName =
    mirror match {
      case declaredType: DeclaredType =>
        val element = declaredType.asElement().asInstanceOf[TypeElement]
        val typeArguments = declaredType.getTypeArguments
        if (typeArguments.isEmpty) {
          TypeName.get(mirror)
        } else {
          val properArguments =
            typeArguments.asScala.map(getConfigClassName).toArray
          ParameterizedTypeName.get(ClassName.get(element), properArguments*)
        }
      case _ =>
        TypeName.get(mirror)
    }

  def getConfigPropertyClassName(mirror: TypeMirror): TypeName =
    getPropertyClassName(
      mirror,
      translateConfigClassName,
      getConfigPropertyClassName
    )

  def publicPropertyClassName(p: DomainProperty): TypeName =
    publicPropertyClassName(p.typeMirror)

  def publicPropertyClassName(mirror: TypeMirror): TypeName =
    getPropertyClassName(mirror, getPublicClassName, publicPropertyClassName)

  private def getPropertyClassName(
      mirror: TypeMirror,
      astMapper: ConfigStructure => ClassName,
      recursiveMapper: TypeMirror => TypeName
  ): TypeName =
    configNameCache.lookupDomain(mirror) match {
      case Some(ast) => astMapper(ast)
      case None      => translateDTOParameters(mirror, recursiveMapper)
    }

  def generateConfigurationClassName(configDTOType: TypeElement): ClassName =
    if (configDTOType.getNestingKind == NestingKind.MEMBER) {
      val enclosingElement = configDTOType.getEnclosingElement
      generateConfigurationClassName(enclosingElement.asInstanceOf[TypeElement])
        .nestedClass(findConfigClassName(configDTOType))
    } else {
      val packageName = TypeElementWrapper.wrap(configDTOType).getPackageName
      ClassName.get(packageName, findConfigClassName(configDTOType))
    }

  def getDeserializerProviderFieldName(typeMirror: TypeMirror): String =
    getDeserializerFieldName(typeMirror) + PROVIDER_SUFFIX

  def getSerializerProviderFieldName(typeMirror: TypeMirror): String =
    getSerializerFieldName(typeMirror) + PROVIDER_SUFFIX

  def getValidatorFieldName(property: DomainProperty): String =
    property.name + VALIDATOR_SUFFIX

  def getValidatorElementFieldName(property: DomainProperty): String =
    property.name + "Element" + VALIDATOR_SUFFIX

  def getValidatorKeyFieldName(property: DomainProperty): String =
    property.name + "Key" + VALIDATOR_SUFFIX

  def getValidatorErrorFieldName(property: DomainProperty): String =
    property.name + "ValidationError"

  def getValidatorElementErrorFieldName(property: DomainProperty): String =
    property.name + "ElementValidationError"

  def getValidatorKeyErrorFieldName(property: DomainProperty): String =
    property.name + "KeyValidationError"

  def getDefaultMethodAccessClassName(ast: ConfigStructure): ClassName =
    val concreteConfigClassName = getConcreteConfigClassName(ast)
    val simpleName = ast.name.simpleName()
    val cleanName =
      if (simpleName.endsWith("DTO")) simpleName.dropRight(3) else simpleName
    concreteConfigClassName.nestedClass(
      cleanName + DEFAULT_METHOD_ACCESS_SUFFIX
    )

  def getLoaderModuleClassName(packageName: String): ClassName =
    ClassName.get(packageName, CONFIG_LOADER_MODULE_NAME)

  def getProvidesProviderMethodName(simpleName: String): String =
    "provide" + simpleName + PROVIDER_SUFFIX

  def getProvidesMethodName(simpleName: String): String =
    "provide" + simpleName

  def getProvidesToConfigSetMethodName(simpleName: String): String =
    "provide" + simpleName + "ToConfigSet"

  def getProvidesToProviderSetMethodName(simpleName: String): String =
    "provide" + simpleName + "ToProviderSet"

  def getDeserializerClassName(ast: ConfigStructure): ClassName =
    val simpleName = ast.name.simpleName()
    val cleanName =
      if (simpleName.endsWith("DTO")) simpleName.dropRight(3)
      else simpleName
    val baseName = cleanName + DESERIALIZER_SUFFIX
    val enclosing = ast.name.enclosingClassName()
    if (enclosing != null) {
      configNameCache.lookupDomain(enclosing) match {
        case Some(enclosingAst) =>
          getDeserializerClassName(enclosingAst).nestedClass(baseName)
        case None => enclosing.nestedClass(baseName)
      }
    } else {
      ast.name.peerClass(baseName)
    }

  def getDeserializerClassName(typeMirror: TypeMirror): ClassName =
    val ast = configNameCache
      .lookupDomain(typeMirror)
      .getOrElse(
        throw new IllegalStateException("Not a config type: " + typeMirror)
      )
    getDeserializerClassName(ast)

  private def getFieldName(typeMirror: TypeMirror, suffix: String): String =
    val ast = configNameCache
      .lookupDomain(typeMirror)
      .getOrElse(
        throw new IllegalStateException("Not a config type: " + typeMirror)
      )
    val publicName = getPublicClassName(ast)
    val safePkg = publicName.packageName().replace('.', '_')
    val prefix = if (safePkg.isEmpty) "" else safePkg + "_"
    Strings.uncapitalize(
      prefix + ConfigurationClassNameGenerator.getCleanSimpleName(publicName)
    ) + suffix

  def getDeserializerFieldName(typeMirror: TypeMirror): String =
    getFieldName(typeMirror, DESERIALIZER_SUFFIX)

  def getSerializerClassName(ast: ConfigStructure): ClassName =
    val simpleName = ast.name.simpleName()
    val cleanName =
      if (simpleName.endsWith("DTO")) simpleName.dropRight(3) else simpleName
    val baseName = cleanName + SERIALIZER_SUFFIX
    val enclosing = ast.name.enclosingClassName()
    if (enclosing != null) {
      configNameCache.lookupDomain(enclosing) match {
        case Some(enclosingAst) =>
          getSerializerClassName(enclosingAst).nestedClass(baseName)
        case None => enclosing.nestedClass(baseName)
      }
    } else {
      ast.name.peerClass(baseName)
    }

  def getSerializerFieldName(typeMirror: TypeMirror): String =
    getFieldName(typeMirror, SERIALIZER_SUFFIX)

  def getValidatorClassName(ast: ConfigStructure): ClassName =
    val simpleName = ast.name.simpleName()
    val cleanName =
      if (simpleName.endsWith("DTO")) simpleName.dropRight(3) else simpleName
    val baseName = cleanName + VALIDATOR_SUFFIX
    val enclosing = ast.name.enclosingClassName()
    if (enclosing != null) {
      configNameCache.lookupDomain(enclosing) match {
        case Some(enclosingAst) =>
          getValidatorClassName(enclosingAst).nestedClass(baseName)
        case None => enclosing.nestedClass(baseName)
      }
    } else {
      ast.name.peerClass(baseName)
    }

  def getInnerDaoName(ast: ConfigStructure): ClassName =
    ast match {
      case a: ConfigStructure.Atomic if a.isInterface =>
        val hasAnyDefaultValue = ast.properties
          .exists(configStructureAnalysis.hasDefaultOrIsInitializable)
        if (!hasAnyDefaultValue) null
        else getDefaultMethodAccessClassName(ast)
      case _ => null
    }
