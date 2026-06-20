package me.bristermitten.mittenlib.annotations.compile

import com.google.inject.Inject
import com.palantir.javapoet.*
import io.toolisticon.aptk.tools.TypeMirrorWrapper

import java.util.{
  ArrayList as JArrayList,
  List as JList,
  Map as JMap,
  Optional as JOptional
}
import javax.annotation.processing.Generated
import javax.lang.model.element.Modifier
import javax.lang.model.`type`.{DeclaredType, TypeMirror}
import me.bristermitten.mittenlib.annotations.domain.{
  ConfigStructure,
  Constraint,
  Property,
  PropertyType
}
import me.bristermitten.mittenlib.annotations.config.ConfigProcessor
import me.bristermitten.mittenlib.annotations.util.TypesUtil
import me.bristermitten.mittenlib.config.exception.ConfigValidationException
import me.bristermitten.mittenlib.util.Result

import scala.jdk.CollectionConverters.*
import _root_.me.bristermitten.mittenlib.codegen.dsl.BlockBuilder.*
import _root_.me.bristermitten.mittenlib.codegen.dsl.{*, given}

import scala.annotation.tailrec

class ConfigValidatorGenerator @Inject (
    private val classNameGenerator: ConfigurationClassNameGenerator,
    private val fieldNameGenerator: FieldNameGenerator,
    private val methodNames: MethodNames,
    private val typesUtil: TypesUtil
):

  @tailrec
  private def getConstraints(pt: PropertyType): List[Constraint] = pt match {
    case PropertyType.Primitive(_, cs)       => cs
    case PropertyType.Object(_, cs)          => cs
    case PropertyType.ListProperty(_, cs)    => cs
    case PropertyType.Set(_, cs)             => cs
    case PropertyType.Map(_, _, cs)          => cs
    case PropertyType.ConfigProperty(_, cs)  => cs
    case PropertyType.EnumType(_, _, cs)     => cs
    case PropertyType.OptionalProperty(elem) => getConstraints(elem)
  }

  private def getKeyConstraints(pt: PropertyType): List[Constraint] = pt match {
    case PropertyType.Map(key, _, _) => getConstraints(key)
    case _                           => Nil
  }

  private def getElementConstraints(pt: PropertyType): List[Constraint] =
    pt match {
      case PropertyType.ListProperty(elem, _) => getConstraints(elem)
      case PropertyType.Set(elem, _)          => getConstraints(elem)
      case PropertyType.Map(_, value, _)      => getConstraints(value)
      case _                                  => Nil
    }

  private case class CustomValidatorTarget(
      validatorClassName: ClassName,
      fieldName: String
  )

  private def getCustomValidatorTargets(
      property: Property
  ): List[CustomValidatorTarget] =
    val pt = property.propertyType
    val main = getConstraints(pt).collect { case Constraint.Custom(className) =>
      CustomValidatorTarget(
        className,
        classNameGenerator.getValidatorFieldName(property)
      )
    }
    val key = getKeyConstraints(pt).collect {
      case Constraint.Custom(className) =>
        CustomValidatorTarget(
          className,
          classNameGenerator.getValidatorKeyFieldName(property)
        )
    }
    val element = getElementConstraints(pt).collect {
      case Constraint.Custom(className) =>
        CustomValidatorTarget(
          className,
          classNameGenerator.getValidatorElementFieldName(property)
        )
    }
    main ++ key ++ element

  def emit(ast: ConfigStructure): JavaFile =
    val validatorClassName = classNameGenerator.getValidatorClassName(ast)
    val builder = createValidatorBuilder(ast)
    addChildValidatorClasses(ast, builder)

    JavaFile
      .builder(validatorClassName.packageName(), builder.build())
      .skipJavaLangImports(true)
      .build()

  private def addChildValidatorClasses(
      ast: ConfigStructure,
      builder: TypeSpec.Builder
  ): Unit =
    for (child <- ast.enclosed) {
      val childBuilder = createValidatorBuilder(child)
      addChildValidatorClasses(child, childBuilder)
      builder.addType(childBuilder.build())
    }

  private def createValidatorBuilder(
      ast: ConfigStructure
  ): TypeSpec.Builder =
    val validatorClassName = classNameGenerator.getValidatorClassName(ast)
    val publicClassName = classNameGenerator.getPublicClassName(ast)

    val builder = TypeSpec
      .classBuilder(validatorClassName.simpleName())
      .addJavadoc(
        """Validator implementation for {@link $T}.
          |""".stripMargin,
        publicClassName
      )
      .addModifiers(Modifier.PUBLIC)

    if (ast.name.enclosingClassName() != null) {
      builder.addModifiers(Modifier.STATIC)
    }

    builder.addAnnotation(GeneratorUtil.generatedAnnotation())

    // Fields
    addValidatorFields(ast, builder)

    // Constructor 1: Guice @Inject constructor
    addGuiceConstructor(ast, builder)

    // validate() method
    addValidateMethod(ast, builder, publicClassName)

    builder

  private def addValidatorFields(
      ast: ConfigStructure,
      builder: TypeSpec.Builder
  ): Unit =
    for
      property <- ast.properties
      target <- getCustomValidatorTargets(property)
    do
      val validatorField = FieldSpec
        .builder(
          target.validatorClassName,
          target.fieldName,
          Modifier.PRIVATE,
          Modifier.FINAL
        )
        .build()
      builder.addField(validatorField)

  private def addGuiceConstructor(
      ast: ConfigStructure,
      builder: TypeSpec.Builder
  ): Unit =
    val constructor = MethodSpec
      .constructorBuilder()
      .addJavadoc(
        "Constructs a new validator instance with its custom field/element/key validators.\n"
      )
      .addAnnotation(classOf[Inject])
      .addModifiers(Modifier.PUBLIC)

    var hasCustom = false
    for
      property <- ast.properties
      target <- getCustomValidatorTargets(property)
    do
      constructor.addParameter(target.validatorClassName, target.fieldName)
      constructor.addStatement(
        "this.$L = $L",
        target.fieldName,
        target.fieldName
      )
      hasCustom = true

    if (hasCustom) {
      builder.addMethod(constructor.build())
    }

  private def bracketedKey(base: Expr, index: Expr): Expr =
    Expr.BinaryOp(
      Expr.BinaryOp(base, "+", Expr.str("[")),
      "+",
      Expr.BinaryOp(index, "+", Expr.str("]"))
    )

  private def addValidateMethod(
      ast: ConfigStructure,
      builder: TypeSpec.Builder,
      publicClassName: ClassName
  ): Unit =
    val configParam = Var("config", TypeRef.of(publicClassName))
    val validateMethodDecl = MethodDecl.build(
      name = "validate",
      returnType = Types.Result(TypeRef.of(publicClassName)),
      parameters = List(configParam),
      modifiers = List(Modifier.PUBLIC),
      annotations = Nil
    ) {
      val violations = declare(
        Types.List(Types.Violation),
        "violations",
        Expr.new_(Types.ArrayList(Types.Violation))
      )

      for (property <- ast.properties) {
        val constraints = getConstraints(property.propertyType)
        val keyConstraints = getKeyConstraints(property.propertyType)
        val elementConstraints = getElementConstraints(property.propertyType)
        val isNullable = property.isNullable
        val isOptional =
          property.propertyType.isInstanceOf[PropertyType.OptionalProperty]
        val isPrimitive = property.propertyType match {
          case PropertyType.Primitive(_, _) => true
          case _                            => false
        }
        val accessorCall = Expr.MethodCall(
          configParam,
          methodNames.safeMethodName(property),
          Nil
        )
        val configKey =
          Expr.str(fieldNameGenerator.getConfigFieldName(property))

        val hasOtherConstraints =
          constraints.nonEmpty || keyConstraints.nonEmpty || elementConstraints.nonEmpty

        def generatePropertyConstraints()(using b: BlockBuilder): Unit = {
          if (constraints.nonEmpty) {
            def runChecks(valueExpr: Expr): Unit = {
              for (constraint <- constraints) {
                generateConstraintCheck(
                  constraint,
                  valueExpr,
                  configKey,
                  Expr.This.field(
                    classNameGenerator.getValidatorFieldName(property)
                  ),
                  classNameGenerator.getValidatorErrorFieldName(property),
                  "",
                  violations
                )
              }
            }

            if (isOptional) {
              ifThen(accessorCall.call("isPresent")) {
                runChecks(accessorCall.call("get"))
              }
            } else {
              runChecks(accessorCall)
            }
          }

          val isList =
            property.propertyType.isInstanceOf[PropertyType.ListProperty]
          val isSet = property.propertyType.isInstanceOf[PropertyType.Set]

          if ((isList || isSet) && elementConstraints.nonEmpty) {
            val wrappedCollection = TypeMirrorWrapper.wrap(property.typeMirror)
            val elementType = wrappedCollection.getTypeArguments.get(0)
            val elementTypeName = TypeName.get(elementType)
            val isElementNullable = typesUtil.isNullable(elementType)
            val isElementPrimitive = elementTypeName.isPrimitive

            if (isSet) {
              forEach(
                TypeRef.of(elementTypeName),
                accessorCall,
                Some("element")
              ) { element =>
                generatePropertyElementValidation(
                  property,
                  element,
                  bracketedKey(configKey, element),
                  isElementPrimitive,
                  isElementNullable,
                  elementConstraints,
                  violations
                )
              }
            } else {
              val i = declare(Types.Int, "i", Expr.int(0))
              forEach(
                TypeRef.of(elementTypeName),
                accessorCall,
                Some("element")
              ) { element =>
                generatePropertyElementValidation(
                  property,
                  element,
                  bracketedKey(configKey, i),
                  isElementPrimitive,
                  isElementNullable,
                  elementConstraints,
                  violations
                )
                assign(i, Expr.BinaryOp(i, "+", Expr.int(1)))
              }
            }
          } else if (
            property.propertyType.isInstanceOf[
              PropertyType.Map
            ] && (keyConstraints.nonEmpty || elementConstraints.nonEmpty)
          ) {
            val wrappedMap = TypeMirrorWrapper.wrap(property.typeMirror)
            val keyType = wrappedMap.getTypeArguments.get(0)
            val valType = wrappedMap.getTypeArguments.get(1)
            val keyTypeName = TypeName.get(keyType)
            val valTypeName = TypeName.get(valType)

            val isKeyNullable = typesUtil.isNullable(keyType)
            val isKeyPrimitive = keyTypeName.isPrimitive
            val isValNullable = typesUtil.isNullable(valType)
            val isValPrimitive = valTypeName.isPrimitive

            val entryTypeName = ParameterizedTypeName.get(
              ClassName.get(classOf[JMap.Entry[?, ?]]),
              keyTypeName,
              valTypeName
            )

            forEach(
              TypeRef.of(entryTypeName),
              accessorCall.call("entrySet"),
              Some("entry")
            ) { entry =>
              val key =
                declare(
                  TypeRef.of(keyTypeName),
                  "key",
                  entry.call("getKey")
                )
              val value =
                declare(
                  TypeRef.of(valTypeName),
                  "value",
                  entry.call("getValue")
                )

              if (keyConstraints.nonEmpty) {
                generateElementValidation(
                  valueExpr = key,
                  keyExpr = bracketedKey(configKey, key),
                  isPrimitive = isKeyPrimitive,
                  isNullable = isKeyNullable,
                  nullMessage = "Key must not be null",
                  constraints = keyConstraints,
                  validatorField = Expr.This.field(
                    classNameGenerator.getValidatorKeyFieldName(property)
                  ),
                  errorFieldName =
                    classNameGenerator.getValidatorKeyErrorFieldName(property),
                  messagePrefix = "Key ",
                  violations = violations
                )
              }

              if (elementConstraints.nonEmpty) {
                generatePropertyElementValidation(
                  property,
                  value,
                  bracketedKey(configKey, key),
                  isValPrimitive,
                  isValNullable,
                  elementConstraints,
                  violations
                )
              }
            }
          }
        }

        if (hasOtherConstraints || !isNullable) {
          if (!isPrimitive) {
            if (!isNullable) {
              def addNullViolation()(using BlockBuilder): Unit =
                statement(
                  violations.call(
                    "add",
                    Expr.new_(
                      Types.Violation,
                      configKey,
                      Expr.Null,
                      Expr.str("Must not be null")
                    )
                  )
                )

              if (hasOtherConstraints) {
                ifThenElse(accessorCall.isNull) {
                  addNullViolation()
                  buildOpen()
                } {
                  generatePropertyConstraints()
                  buildOpen()
                }
              } else {
                ifThen(accessorCall.isNull) {
                  addNullViolation()
                }
              }
            } else {
              ifThen(accessorCall.isNotNull) {
                generatePropertyConstraints()
              }
            }
          } else {
            generatePropertyConstraints()
          }
        }
      }

      ifThen(!violations.call("isEmpty")) {
        return_(
          Expr.staticCall(
            Types.Result,
            "fail",
            Expr.new_(
              Types.ConfigValidationException,
              configParam.call("getClass"),
              violations
            )
          )
        )
      }

      return_(Expr.staticCall(Types.Result, "ok", configParam))
    }

    val methodSpec = CodeBlockRenderer.renderMethod(validateMethodDecl)
    builder.addMethod(methodSpec)

  private def generatePropertyElementValidation(
      property: Property,
      valueExpr: Expr,
      keyExpr: Expr,
      isElementPrimitive: Boolean,
      isElementNullable: Boolean,
      elementConstraints: List[Constraint],
      violations: Var
  )(using BlockBuilder): Unit =
    generateElementValidation(
      valueExpr = valueExpr,
      keyExpr = keyExpr,
      isPrimitive = isElementPrimitive,
      isNullable = isElementNullable,
      nullMessage = "Must not be null",
      constraints = elementConstraints,
      validatorField = Expr.This.field(
        classNameGenerator.getValidatorElementFieldName(property)
      ),
      errorFieldName = classNameGenerator
        .getValidatorElementErrorFieldName(property),
      messagePrefix = "",
      violations = violations
    )

  private def generateElementValidation(
      valueExpr: Expr,
      keyExpr: Expr,
      isPrimitive: Boolean,
      isNullable: Boolean,
      nullMessage: String,
      constraints: List[Constraint],
      validatorField: Expr,
      errorFieldName: String,
      messagePrefix: String,
      violations: Var
  )(using b: BlockBuilder): Unit =
    if (!isPrimitive) {
      if (!isNullable) {
        ifThenElse(valueExpr.isNull) {
          statement(
            violations.call(
              "add",
              Expr.new_(
                Types.Violation,
                keyExpr,
                Expr.Null,
                Expr.str(nullMessage)
              )
            )
          )
          buildOpen()
        } {
          for (constraint <- constraints) {
            generateConstraintCheck(
              constraint,
              valueExpr,
              keyExpr,
              validatorField,
              errorFieldName,
              messagePrefix,
              violations
            )
          }
          buildOpen()
        }
      } else {
        ifThen(valueExpr.isNotNull) {
          for (constraint <- constraints) {
            generateConstraintCheck(
              constraint,
              valueExpr,
              keyExpr,
              validatorField,
              errorFieldName,
              messagePrefix,
              violations
            )
          }
        }
      }
    } else {
      for (constraint <- constraints) {
        generateConstraintCheck(
          constraint,
          valueExpr,
          keyExpr,
          validatorField,
          errorFieldName,
          messagePrefix,
          violations
        )
      }
    }

  private def addViolation(
      violations: Var,
      keyExpr: Expr,
      valueExpr: Expr,
      messageExpr: Expr
  )(using BlockBuilder): Unit =
    statement(
      violations.call(
        "add",
        Expr.new_(
          Types.Violation,
          keyExpr,
          valueExpr,
          messageExpr
        )
      )
    )

  private def generateConstraintCheck(
      constraint: Constraint,
      valueExpr: Expr,
      keyExpr: Expr,
      validatorField: Expr,
      errorFieldName: String,
      messagePrefix: String,
      violations: Var
  )(using b: BlockBuilder): Unit =
    constraint match {
      case Constraint.Positive =>
        ifThen(valueExpr <= Expr.int(0)) {
          addViolation(
            violations,
            keyExpr,
            valueExpr,
            Expr.str(messagePrefix + "Must be positive")
          )
        }

      case Constraint.Negative =>
        ifThen(valueExpr >= Expr.int(0)) {
          addViolation(
            violations,
            keyExpr,
            valueExpr,
            Expr.str(messagePrefix + "Must be negative")
          )
        }

      case Constraint.Min(minVal) =>
        val limit = Expr.Literal(minVal.toString)
        ifThen(valueExpr < limit) {
          addViolation(
            violations,
            keyExpr,
            valueExpr,
            Expr.BinaryOp(
              Expr.str(messagePrefix + "Must be at least "),
              "+",
              limit
            )
          )
        }

      case Constraint.Max(maxVal) =>
        val limit = Expr.Literal(maxVal.toString)
        ifThen(valueExpr > limit) {
          addViolation(
            violations,
            keyExpr,
            valueExpr,
            Expr.BinaryOp(
              Expr.str(messagePrefix + "Must be at most "),
              "+",
              limit
            )
          )
        }

      case Constraint.Range(minVal, maxVal) =>
        val minLimit = Expr.Literal(minVal.toString)
        val maxLimit = Expr.Literal(maxVal.toString)
        ifThen(valueExpr < minLimit || valueExpr > maxLimit) {
          addViolation(
            violations,
            keyExpr,
            valueExpr,
            Expr.BinaryOp(
              Expr.BinaryOp(
                Expr.str(messagePrefix + "Must be between "),
                "+",
                minLimit
              ),
              "+",
              Expr.BinaryOp(Expr.str(" and "), "+", maxLimit)
            )
          )
        }

      case Constraint.NotBlank =>
        ifThen(valueExpr.call("trim").call("isEmpty")) {
          addViolation(
            violations,
            keyExpr,
            valueExpr,
            Expr.str(messagePrefix + "Must not be blank")
          )
        }

      case Constraint.Custom(validatorClassName) =>
        val errorVar = declare(
          TypeRef.of(
            ParameterizedTypeName.get(
              ClassName.get(classOf[java.util.Optional[?]]),
              ClassName.get(classOf[java.lang.String])
            )
          ),
          errorFieldName,
          validatorField.call("validate", valueExpr)
        )
        ifThen(errorVar.call("isPresent")) {
          val msg = if (messagePrefix.isEmpty) {
            errorVar.call("get")
          } else {
            Expr.BinaryOp(Expr.str(messagePrefix), "+", errorVar.call("get"))
          }
          addViolation(violations, keyExpr, valueExpr, msg)
        }
    }
