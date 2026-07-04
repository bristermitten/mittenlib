package me.bristermitten.mittenlib.annotations.util

import me.bristermitten.mittenlib.config.names.ConfigName

object PrivateAnnotations:
  private val PRIVATE_ANNOTATIONS = Set(
    classOf[ConfigName].getName,
    classOf[Override].getName
  )

  def isPrivate(annotation: String): Boolean =
    PRIVATE_ANNOTATIONS.contains(annotation)
