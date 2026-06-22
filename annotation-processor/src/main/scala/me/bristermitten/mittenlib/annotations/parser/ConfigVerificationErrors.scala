package me.bristermitten.mittenlib.annotations.parser

import io.toolisticon.aptk.tools.corematcher.PlainValidationMessage
import io.toolisticon.aptk.tools.corematcher.ValidationMessage

object ConfigVerificationErrors:
  val UNION_ALTERNATIVE_NOT_EXTENDING_UNION: ValidationMessage =
    PlainValidationMessage.create(
      "UNION_ALTERNATIVE_NOT_EXTENDING_UNION",
      "Alternative in union ${0} MUST extend the union type when the union type has properties defined!"
    )

  val ENUM_PARSING_SCHEME_NOT_ENUM: ValidationMessage =
    PlainValidationMessage.create(
      "ENUM_PARSING_SCHEME_NOT_ENUM",
      "This property's type is not an enum, so the @EnumParsingScheme annotation will have no effect."
    )

  val CLASS_DTO_MISSING_NO_ARG_CONSTRUCTOR: ValidationMessage =
    PlainValidationMessage.create(
      "CLASS_DTO_MISSING_NO_ARG_CONSTRUCTOR",
      "Class DTO ${0} has fields with default values, but is missing an accessible (non-private) zero-arguments constructor. Please define an accessible zero-arguments constructor (package-private, protected, or public) so MittenLib can read the default values."
    )

  val CONSTRAINT_TYPE_MISMATCH: ValidationMessage =
    PlainValidationMessage.create(
      "CONSTRAINT_TYPE_MISMATCH",
      "Constraint annotation ${0} cannot be applied to type ${1}. Expected a ${2}."
    )

  val SERIALIZATION_NOT_SUPPORTED: ValidationMessage =
    PlainValidationMessage.create(
      "SERIALIZATION_NOT_SUPPORTED",
      "Serialization is required for this config, but it contains properties that cannot be serialized: ${0}"
    )

  val SERIALIZATION_NOT_SUPPORTED_WARNING: ValidationMessage =
    PlainValidationMessage.create(
      "SERIALIZATION_NOT_SUPPORTED_WARNING",
      "This config contains properties that cannot be serialized: ${0}. Attempting to serialize this config may result in runtime exceptions. To make this a compile-time error, set requireSerialization to true."
    )

  val NOT_DYNAMICALLY_INITIALIZABLE: ValidationMessage =
    PlainValidationMessage.create(
      "NOT_DYNAMICALLY_INITIALIZABLE",
      "Config ${0} has a @Source but is not dynamically initializable because the following required properties lack default values: ${1}. " +
        "You must provide a default configuration file (e.g. ${2}) in your jar's resources, " +
        "or provide default values for these properties to avoid runtime errors. If you understand the risks but do not want to change the type, set requireDynamicInitialization to false to set this to a warning rather than error."
    )
