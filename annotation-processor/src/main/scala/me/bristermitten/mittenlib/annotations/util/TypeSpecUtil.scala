package me.bristermitten.mittenlib.annotations.util

import com.palantir.javapoet.AnnotationSpec
import com.palantir.javapoet.MethodSpec
import com.palantir.javapoet.TypeName
import java.lang.annotation.Repeatable
import java.util.function.Consumer
import scala.jdk.CollectionConverters.*

object TypeSpecUtil:

  def methodAddAnnotation(
      builder: MethodSpec.Builder,
      annotation: Class[?]
  ): Unit =
    methodAddAnnotation(builder, annotation, _ => ())

  def methodAddAnnotation(
      builder: MethodSpec.Builder,
      annotation: Class[?],
      builderConsumer: Consumer[AnnotationSpec.Builder]
  ): Unit =
    val hasAnnotation = builder
      .build()
      .annotations
      .asScala
      .exists(existing => existing.`type`().equals(TypeName.get(annotation)))

    if (!hasAnnotation || annotation.isAnnotationPresent(classOf[Repeatable])) {
      val annotationBuilder = AnnotationSpec.builder(annotation)
      builderConsumer.accept(annotationBuilder)
      builder.addAnnotation(annotationBuilder.build())
    }
