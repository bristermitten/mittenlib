package me.bristermitten.mittenlib.codegen.union

import com.palantir.javapoet.ClassName
import java.util.List
import me.bristermitten.mittenlib.codegen.GenericTypeSpec
import me.bristermitten.mittenlib.codegen.MatchStrategies

case class ResolvedUnionSpec(
  source: ClassName,
  name: ClassName,
  strategy: MatchStrategies,
  constructors: List[ResolvedUnionConstructor]
) extends GenericTypeSpec
