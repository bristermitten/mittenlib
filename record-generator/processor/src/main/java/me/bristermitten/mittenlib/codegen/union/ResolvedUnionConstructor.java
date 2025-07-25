package me.bristermitten.mittenlib.codegen.union;

import com.squareup.javapoet.ClassName;
import me.bristermitten.mittenlib.codegen.record.RecordConstructorSpec;
import me.bristermitten.mittenlib.codegen.record.RecordSpecLike;

public record ResolvedUnionConstructor(
        ClassName source,
        ClassName name,
        RecordConstructorSpec constructor
) implements RecordSpecLike {
}