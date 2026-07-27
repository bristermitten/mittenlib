package me.bristermitten.mittenlib.annotations.util

import io.toolisticon.aptk.tools.AnnotationUtils
import javax.lang.model.element.{AnnotationMirror, Element}
import javax.lang.model.`type`.{MirroredTypeException, TypeMirror}
import java.lang.annotation.Annotation
import scala.reflect.ClassTag

object AnnotationMirrorUtil:

  /** Safely extracts a [[TypeMirror]] attribute from an annotation instance by
    * handling [[MirroredTypeException]].
    */
  def extractTypeMirror[A <: Annotation](
      element: Element,
      annotationClass: Class[A]
  )(
      accessor: A => Class[?]
  ): Option[TypeMirror] =
    val annotation = element.getAnnotation(annotationClass)
    if (annotation == null) None
    else {
      try {
        val _ = accessor(annotation)
        None
      } catch {
        case e: MirroredTypeException => Some(e.getTypeMirror)
      }
    }
