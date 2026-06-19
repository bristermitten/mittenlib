package me.bristermitten.mittenlib.codegen.record

import com.palantir.javapoet.TypeName

case class RecordConstructorSpec(
    name: String,
    fields: java.util.List[RecordConstructorSpec.RecordFieldSpec]
)

object RecordConstructorSpec:
  case class RecordFieldSpec(name: String, `type`: TypeName)
