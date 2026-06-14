package me.bristermitten.mittenlib.codegen.union

import com.palantir.javapoet.ClassName
import java.util.List
import me.bristermitten.mittenlib.codegen.GenericTypeSpec
import me.bristermitten.mittenlib.codegen.MatchStrategies
import me.bristermitten.mittenlib.codegen.record.RecordConstructorSpec

case class UnionSpec(
  source: ClassName,
  name: ClassName,
  strategy: MatchStrategies,
  constructors: List[RecordConstructorSpec]
) extends GenericTypeSpec
