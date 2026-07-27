package me.bristermitten.mittenlib.annotations.parser

import com.google.inject.Singleton
import com.palantir.javapoet.ClassName
import me.bristermitten.mittenlib.annotations.domain.Constraint
import javax.lang.model.AnnotatedConstruct
import javax.lang.model.element.{AnnotationMirror, TypeElement}
import javax.lang.model.`type`.{DeclaredType, TypeMirror}
import scala.jdk.CollectionConverters.*

@Singleton
class ConstraintChecker:

  def parseConstraints(construct: AnnotatedConstruct): List[Constraint] =
    construct.getAnnotationMirrors.asScala.flatMap { mirror =>
      val qName = mirror.getAnnotationType.asElement
        .asInstanceOf[TypeElement]
        .getQualifiedName
        .toString
      qName match {
        case "me.bristermitten.mittenlib.config.validation.Positive" |
            "jakarta.validation.constraints.Positive" |
            "javax.validation.constraints.Positive" =>
          Some(Constraint.Positive)
        case "me.bristermitten.mittenlib.config.validation.Negative" |
            "jakarta.validation.constraints.Negative" |
            "javax.validation.constraints.Negative" =>
          Some(Constraint.Negative)
        case "me.bristermitten.mittenlib.config.validation.Min" |
            "jakarta.validation.constraints.Min" |
            "javax.validation.constraints.Min" =>
          val value = getAnnotationNumericValue(mirror, "value").getOrElse(0.0)
          Some(Constraint.Min(value))
        case "me.bristermitten.mittenlib.config.validation.Max" |
            "jakarta.validation.constraints.Max" |
            "javax.validation.constraints.Max" =>
          val value = getAnnotationNumericValue(mirror, "value").getOrElse(0.0)
          Some(Constraint.Max(value))
        case "me.bristermitten.mittenlib.config.validation.Range" |
            "org.hibernate.validator.constraints.Range" =>
          val min = getAnnotationNumericValue(mirror, "min").getOrElse(0.0)
          val max = getAnnotationNumericValue(mirror, "max").getOrElse(0.0)
          Some(Constraint.Range(min, max))
        case "me.bristermitten.mittenlib.config.validation.NotBlank" |
            "jakarta.validation.constraints.NotBlank" |
            "javax.validation.constraints.NotBlank" =>
          Some(Constraint.NotBlank)
        case "me.bristermitten.mittenlib.config.validation.ValidateWith" =>
          getAnnotationValue[TypeMirror](mirror, "value").map { tpe =>
            Constraint.Custom(
              ClassName.get(
                tpe
                  .asInstanceOf[DeclaredType]
                  .asElement()
                  .asInstanceOf[TypeElement]
              )
            )
          }
        case _ => None
      }
    }.toList

  private def getAnnotationValue[T](
      mirror: AnnotationMirror,
      key: String
  ): Option[T] =
    mirror.getElementValues.asScala
      .find { case (element, _) =>
        element.getSimpleName.toString == key
      }
      .map { case (_, value) =>
        value.getValue.asInstanceOf[T]
      }

  private def getAnnotationNumericValue(
      mirror: AnnotationMirror,
      key: String
  ): Option[Double] =
    mirror.getElementValues.asScala
      .find { case (element, _) =>
        element.getSimpleName.toString == key
      }
      .flatMap { case (_, value) =>
        value.getValue match {
          case n: Number => Some(n.doubleValue())
          case _         => None
        }
      }
