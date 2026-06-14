package me.bristermitten.mittenlib.codegen.record

import com.palantir.javapoet.ClassName
import me.bristermitten.mittenlib.codegen.GenericTypeSpec

trait RecordSpecLike extends GenericTypeSpec:
  def source: ClassName
  def constructor: RecordConstructorSpec
