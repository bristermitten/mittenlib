package me.bristermitten.mittenlib.codegen.record

import com.palantir.javapoet.TypeName
import java.util.List

case class RecordConstructorSpec(name: String, fields: List[RecordConstructorSpec.RecordFieldSpec])

object RecordConstructorSpec:
  case class RecordFieldSpec(name: String, `type`: TypeName)
