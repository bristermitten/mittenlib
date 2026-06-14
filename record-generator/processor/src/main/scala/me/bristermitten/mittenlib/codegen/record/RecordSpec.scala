package me.bristermitten.mittenlib.codegen.record

import com.palantir.javapoet.ClassName
import me.bristermitten.mittenlib.codegen.GenericTypeSpec

case class RecordSpec(source: ClassName, name: ClassName, constructor: RecordConstructorSpec)
  extends GenericTypeSpec with RecordSpecLike
