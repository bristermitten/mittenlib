package me.bristermitten.mittenlib.codegen.union

import com.palantir.javapoet.ClassName
import me.bristermitten.mittenlib.codegen.record.RecordConstructorSpec
import me.bristermitten.mittenlib.codegen.record.RecordSpecLike

case class ResolvedUnionConstructor(source: ClassName, name: ClassName, constructor: RecordConstructorSpec)
  extends RecordSpecLike
