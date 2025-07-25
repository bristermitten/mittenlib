package me.bristermitten.mittenlib.codegen.union;

import com.squareup.javapoet.ClassName;
import me.bristermitten.mittenlib.codegen.GenericTypeSpec;
import me.bristermitten.mittenlib.codegen.MatchStrategies;
import me.bristermitten.mittenlib.codegen.record.RecordConstructorSpec;

import java.util.List;

public record UnionSpec(
        ClassName source,
        ClassName name,
        MatchStrategies strategy,
        List<RecordConstructorSpec> constructors
) implements GenericTypeSpec {

}
