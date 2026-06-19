package me.bristermitten.mittenlib.annotations.compile

import com.google.inject.Inject
import com.palantir.javapoet.*
import java.util.{
  ArrayList => JArrayList,
  List => JList,
  Map => JMap,
  Optional => JOptional
}
import javax.annotation.processing.Generated
import javax.lang.model.element.Modifier
import javax.lang.model.`type`.{DeclaredType, TypeMirror}
import me.bristermitten.mittenlib.annotations.ast.{
  AbstractConfigStructure,
  Property,
  ValidationConstraint
}
import me.bristermitten.mittenlib.annotations.config.ConfigProcessor
import me.bristermitten.mittenlib.annotations.util.TypesUtil
import me.bristermitten.mittenlib.config.exception.ConfigValidationException
import me.bristermitten.mittenlib.util.Result
import scala.jdk.CollectionConverters.*
import _root_.me.bristermitten.mittenlib.codegen.dsl.BlockBuilder.*
import _root_.me.bristermitten.mittenlib.codegen.dsl.{*, given}

class ConfigValidatorGenerator @Inject (
    private val classNameGenerator: ConfigurationClassNameGenerator,
    private val fieldNameGenerator: FieldNameGenerator,
    private val methodNames: MethodNames,
    private val typesUtil: TypesUtil
):

  def emit(ast: AbstractConfigStructure): JavaFile =
    val validatorClassName = classNameGenerator.getValidatorClassName(ast)
    val builder = createValidatorBuilder(ast)
    addChildValidatorClasses(ast, builder)

    JavaFile
      .builder(validatorClassName.packageName(), builder.build())
      .skipJavaLangImports(true)
      .build()

  private def addChildValidatorClasses(
      ast: AbstractConfigStructure,
      builder: TypeSpec.Builder
  ): Unit =
    for (child <- ast.enclosed().asScala) {
      val childBuilder = createValidatorBuilder(child)
      addChildValidatorClasses(child, childBuilder)
      builder.addType(childBuilder.build())
    }

  private def createValidatorBuilder(
      ast: AbstractConfigStructure
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

    if (ast.enclosedIn() != null) {
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
      ast: AbstractConfigStructure,
      builder: TypeSpec.Builder
  ): Unit =
    for (property <- ast.properties().asScala) {
      for (constraint <- property.settings().constraints().asScala) {
        constraint match {
          case custom: ValidationConstraint.Custom =>
            val validatorClassName = custom.validatorClassName()
            val validatorField = FieldSpec
              .builder(
                validatorClassName,
                classNameGenerator.getValidatorFieldName(property),
                Modifier.PRIVATE,
                Modifier.FINAL
              )
              .build()
            builder.addField(validatorField)
          case _ =>
        }
      }
      for (constraint <- property.settings().keyConstraints().asScala) {
        constraint match {
          case custom: ValidationConstraint.Custom =>
            val validatorClassName = custom.validatorClassName()
            val validatorField = FieldSpec
              .builder(
                validatorClassName,
                classNameGenerator.getValidatorKeyFieldName(property),
                Modifier.PRIVATE,
                Modifier.FINAL
              )
              .build()
            builder.addField(validatorField)
          case _ =>
        }
      }
      for (constraint <- property.settings().elementConstraints().asScala) {
        constraint match {
          case custom: ValidationConstraint.Custom =>
            val validatorClassName = custom.validatorClassName()
            val validatorField = FieldSpec
              .builder(
                validatorClassName,
                classNameGenerator.getValidatorElementFieldName(property),
                Modifier.PRIVATE,
                Modifier.FINAL
              )
              .build()
            builder.addField(validatorField)
          case _ =>
        }
      }
    }

  private def addGuiceConstructor(
      ast: AbstractConfigStructure,
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
    for (property <- ast.properties().asScala) {
      for (constraint <- property.settings().constraints().asScala) {
        constraint match {
          case custom: ValidationConstraint.Custom =>
            val validatorClassName = custom.validatorClassName()
            val fieldName = classNameGenerator.getValidatorFieldName(property)
            constructor.addParameter(validatorClassName, fieldName)
            constructor.addStatement("this.$L = $L", fieldName, fieldName)
            hasCustom = true
          case _ =>
        }
      }
      for (constraint <- property.settings().keyConstraints().asScala) {
        constraint match {
          case custom: ValidationConstraint.Custom =>
            val validatorClassName = custom.validatorClassName()
            val fieldName =
              classNameGenerator.getValidatorKeyFieldName(property)
            constructor.addParameter(validatorClassName, fieldName)
            constructor.addStatement("this.$L = $L", fieldName, fieldName)
            hasCustom = true
          case _ =>
        }
      }
      for (constraint <- property.settings().elementConstraints().asScala) {
        constraint match {
          case custom: ValidationConstraint.Custom =>
            val validatorClassName = custom.validatorClassName()
            val fieldName =
              classNameGenerator.getValidatorElementFieldName(property)
            constructor.addParameter(validatorClassName, fieldName)
            constructor.addStatement("this.$L = $L", fieldName, fieldName)
            hasCustom = true
          case _ =>
        }
      }
    }

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
      ast: AbstractConfigStructure,
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

      for (property <- ast.properties().asScala) {
        val constraints = property.settings().constraints()
        val keyConstraints = property.settings().keyConstraints()
        val elementConstraints = property.settings().elementConstraints()
        val isNullable = property.settings().isNullable
        val isPrimitive = TypeName.get(property.propertyType()).isPrimitive
        val accessorCall = Expr.MethodCall(
          configParam,
          methodNames.safeMethodName(property),
          Nil
        )
        val configKey =
          Expr.str(fieldNameGenerator.getConfigFieldName(property))

        val hasOtherConstraints =
          !constraints.isEmpty || !keyConstraints.isEmpty || !elementConstraints.isEmpty

        def generatePropertyConstraints()(using b: BlockBuilder): Unit = {
          if (!constraints.isEmpty) {
            for (constraint <- constraints.asScala) {
              generateConstraintCheck(
                constraint,
                accessorCall,
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

          if (
            typesUtil.isCollection(
              property.propertyType()
            ) && !elementConstraints.isEmpty
          ) {
            val typeArguments = property
              .propertyType()
              .asInstanceOf[DeclaredType]
              .getTypeArguments
            if (!typeArguments.isEmpty) {
              val elementType = typeArguments.get(0)
              val elementTypeName = TypeName.get(elementType)
              val isElementNullable = typesUtil.isNullable(elementType)
              val isElementPrimitive = elementTypeName.isPrimitive

              if (typesUtil.isSet(property.propertyType())) {
                forEach(
                  TypeRef.of(TypeName.get(elementType)),
                  accessorCall,
                  Some("element")
                ) { element =>
                  if (!isElementPrimitive) {
                    if (!isElementNullable) {
                      ifThenElse(element.isNull) {
                        statement(
                          violations.call(
                            "add",
                            Expr.new_(
                              Types.Violation,
                              bracketedKey(configKey, element),
                              Expr.Null,
                              Expr.str("Must not be null")
                            )
                          )
                        )
                        buildOpen()
                      } {
                        for (constraint <- elementConstraints.asScala) {
                          generateConstraintCheck(
                            constraint,
                            element,
                            bracketedKey(configKey, element),
                            Expr.This.field(
                              classNameGenerator.getValidatorElementFieldName(
                                property
                              )
                            ),
                            classNameGenerator
                              .getValidatorElementErrorFieldName(property),
                            "",
                            violations
                          )
                        }
                        buildOpen()
                      }
                    } else {
                      ifThen(element.isNotNull) {
                        for (constraint <- elementConstraints.asScala) {
                          generateConstraintCheck(
                            constraint,
                            element,
                            bracketedKey(configKey, element),
                            Expr.This.field(
                              classNameGenerator.getValidatorElementFieldName(
                                property
                              )
                            ),
                            classNameGenerator
                              .getValidatorElementErrorFieldName(property),
                            "",
                            violations
                          )
                        }
                      }
                    }
                  } else {
                    for (constraint <- elementConstraints.asScala) {
                      generateConstraintCheck(
                        constraint,
                        element,
                        bracketedKey(configKey, element),
                        Expr.This.field(
                          classNameGenerator.getValidatorElementFieldName(
                            property
                          )
                        ),
                        classNameGenerator.getValidatorElementErrorFieldName(
                          property
                        ),
                        "",
                        violations
                      )
                    }
                  }
                }
              } else {
                val i = declare(Types.Int, "i", Expr.int(0))
                forEach(
                  TypeRef.of(TypeName.get(elementType)),
                  accessorCall,
                  Some("element")
                ) { element =>
                  if (!isElementPrimitive) {
                    if (!isElementNullable) {
                      ifThenElse(element.isNull) {
                        statement(
                          violations.call(
                            "add",
                            Expr.new_(
                              Types.Violation,
                              bracketedKey(configKey, i),
                              Expr.Null,
                              Expr.str("Must not be null")
                            )
                          )
                        )
                        buildOpen()
                      } {
                        for (constraint <- elementConstraints.asScala) {
                          generateConstraintCheck(
                            constraint,
                            element,
                            bracketedKey(configKey, i),
                            Expr.This.field(
                              classNameGenerator.getValidatorElementFieldName(
                                property
                              )
                            ),
                            classNameGenerator
                              .getValidatorElementErrorFieldName(property),
                            "",
                            violations
                          )
                        }
                        buildOpen()
                      }
                    } else {
                      ifThen(element.isNotNull) {
                        for (constraint <- elementConstraints.asScala) {
                          generateConstraintCheck(
                            constraint,
                            element,
                            bracketedKey(configKey, i),
                            Expr.This.field(
                              classNameGenerator.getValidatorElementFieldName(
                                property
                              )
                            ),
                            classNameGenerator
                              .getValidatorElementErrorFieldName(property),
                            "",
                            violations
                          )
                        }
                      }
                    }
                  } else {
                    for (constraint <- elementConstraints.asScala) {
                      generateConstraintCheck(
                        constraint,
                        element,
                        bracketedKey(configKey, i),
                        Expr.This.field(
                          classNameGenerator.getValidatorElementFieldName(
                            property
                          )
                        ),
                        classNameGenerator.getValidatorElementErrorFieldName(
                          property
                        ),
                        "",
                        violations
                      )
                    }
                  }
                  assign(i, Expr.BinaryOp(i, "+", Expr.int(1)))
                }
              }
            }
          } else if (
            typesUtil.isMap(
              property.propertyType()
            ) && (!keyConstraints.isEmpty || !elementConstraints.isEmpty)
          ) {
            val typeArguments = property
              .propertyType()
              .asInstanceOf[DeclaredType]
              .getTypeArguments
            if (typeArguments.size() >= 2) {
              val keyType = typeArguments.get(0)
              val valType = typeArguments.get(1)
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
                    TypeRef.of(TypeName.get(keyType)),
                    "key",
                    entry.call("getKey")
                  )
                val value =
                  declare(
                    TypeRef.of(TypeName.get(valType)),
                    "value",
                    entry.call("getValue")
                  )

                if (!keyConstraints.isEmpty) {
                  if (!isKeyPrimitive) {
                    if (!isKeyNullable) {
                      ifThenElse(key.isNull) {
                        statement(
                          violations.call(
                            "add",
                            Expr.new_(
                              Types.Violation,
                              bracketedKey(configKey, key),
                              Expr.Null,
                              Expr.str("Key must not be null")
                            )
                          )
                        )
                        buildOpen()
                      } {
                        for (constraint <- keyConstraints.asScala) {
                          generateConstraintCheck(
                            constraint,
                            key,
                            bracketedKey(configKey, key),
                            Expr.This.field(
                              classNameGenerator.getValidatorKeyFieldName(
                                property
                              )
                            ),
                            classNameGenerator.getValidatorKeyErrorFieldName(
                              property
                            ),
                            "Key ",
                            violations
                          )
                        }
                        buildOpen()
                      }
                    } else {
                      ifThen(key.isNotNull) {
                        for (constraint <- keyConstraints.asScala) {
                          generateConstraintCheck(
                            constraint,
                            key,
                            bracketedKey(configKey, key),
                            Expr.This.field(
                              classNameGenerator.getValidatorKeyFieldName(
                                property
                              )
                            ),
                            classNameGenerator.getValidatorKeyErrorFieldName(
                              property
                            ),
                            "Key ",
                            violations
                          )
                        }
                      }
                    }
                  } else {
                    for (constraint <- keyConstraints.asScala) {
                      generateConstraintCheck(
                        constraint,
                        key,
                        bracketedKey(configKey, key),
                        Expr.This.field(
                          classNameGenerator.getValidatorKeyFieldName(property)
                        ),
                        classNameGenerator.getValidatorKeyErrorFieldName(
                          property
                        ),
                        "Key ",
                        violations
                      )
                    }
                  }
                }

                if (!elementConstraints.isEmpty) {
                  if (!isValPrimitive) {
                    if (!isValNullable) {
                      ifThenElse(value.isNull) {
                        statement(
                          violations.call(
                            "add",
                            Expr.new_(
                              Types.Violation,
                              bracketedKey(configKey, key),
                              Expr.Null,
                              Expr.str("Must not be null")
                            )
                          )
                        )
                        buildOpen()
                      } {
                        for (constraint <- elementConstraints.asScala) {
                          generateConstraintCheck(
                            constraint,
                            value,
                            bracketedKey(configKey, key),
                            Expr.This.field(
                              classNameGenerator.getValidatorElementFieldName(
                                property
                              )
                            ),
                            classNameGenerator
                              .getValidatorElementErrorFieldName(property),
                            "",
                            violations
                          )
                        }
                        buildOpen()
                      }
                    } else {
                      ifThen(value.isNotNull) {
                        for (constraint <- elementConstraints.asScala) {
                          generateConstraintCheck(
                            constraint,
                            value,
                            bracketedKey(configKey, key),
                            Expr.This.field(
                              classNameGenerator.getValidatorElementFieldName(
                                property
                              )
                            ),
                            classNameGenerator
                              .getValidatorElementErrorFieldName(property),
                            "",
                            violations
                          )
                        }
                      }
                    }
                  } else {
                    for (constraint <- elementConstraints.asScala) {
                      generateConstraintCheck(
                        constraint,
                        value,
                        bracketedKey(configKey, key),
                        Expr.This.field(
                          classNameGenerator.getValidatorElementFieldName(
                            property
                          )
                        ),
                        classNameGenerator.getValidatorElementErrorFieldName(
                          property
                        ),
                        "",
                        violations
                      )
                    }
                  }
                }
              }
            }
          }
        }

        if (hasOtherConstraints || !isNullable) {
          if (!isPrimitive) {
            if (!isNullable) {
              if (hasOtherConstraints) {
                ifThenElse(accessorCall.isNull) {
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
                  buildOpen()
                } {
                  generatePropertyConstraints()
                  buildOpen()
                }
              } else {
                ifThen(accessorCall.isNull) {
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

  private def generateConstraintCheck(
      constraint: ValidationConstraint,
      valueExpr: Expr,
      keyExpr: Expr,
      validatorField: Expr,
      errorFieldName: String,
      messagePrefix: String,
      violations: Var
  )(using b: BlockBuilder): Unit =
    constraint match {
      case _: ValidationConstraint.Positive =>
        ifThen(valueExpr <= Expr.int(0)) {
          statement(
            violations.call(
              "add",
              Expr.new_(
                Types.Violation,
                keyExpr,
                valueExpr,
                Expr.str(messagePrefix + "Must be positive")
              )
            )
          )
        }

      case _: ValidationConstraint.Negative =>
        ifThen(valueExpr >= Expr.int(0)) {
          statement(
            violations.call(
              "add",
              Expr.new_(
                Types.Violation,
                keyExpr,
                valueExpr,
                Expr.str(messagePrefix + "Must be negative")
              )
            )
          )
        }

      case min: ValidationConstraint.Min =>
        val valDouble = min.value()
        val limit = Expr.Literal(valDouble.toString)
        ifThen(valueExpr < limit) {
          statement(
            violations.call(
              "add",
              Expr.new_(
                Types.Violation,
                keyExpr,
                valueExpr,
                Expr.BinaryOp(
                  Expr.str(messagePrefix + "Must be at least "),
                  "+",
                  limit
                )
              )
            )
          )
        }

      case max: ValidationConstraint.Max =>
        val valDouble = max.value()
        val limit = Expr.Literal(valDouble.toString)
        ifThen(valueExpr > limit) {
          statement(
            violations.call(
              "add",
              Expr.new_(
                Types.Violation,
                keyExpr,
                valueExpr,
                Expr.BinaryOp(
                  Expr.str(messagePrefix + "Must be at most "),
                  "+",
                  limit
                )
              )
            )
          )
        }

      case range: ValidationConstraint.Range =>
        val minDouble = range.min()
        val maxDouble = range.max()
        val minLimit = Expr.Literal(minDouble.toString)
        val maxLimit = Expr.Literal(maxDouble.toString)
        ifThen(valueExpr < minLimit || valueExpr > maxLimit) {
          statement(
            violations.call(
              "add",
              Expr.new_(
                Types.Violation,
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
            )
          )
        }

      case _: ValidationConstraint.NotBlank =>
        ifThen(valueExpr.call("trim").call("isEmpty")) {
          statement(
            violations.call(
              "add",
              Expr.new_(
                Types.Violation,
                keyExpr,
                valueExpr,
                Expr.str(messagePrefix + "Must not be blank")
              )
            )
          )
        }

      case custom: ValidationConstraint.Custom =>
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
          statement(
            violations.call(
              "add",
              Expr.new_(
                Types.Violation,
                keyExpr,
                valueExpr,
                msg
              )
            )
          )
        }
    }
