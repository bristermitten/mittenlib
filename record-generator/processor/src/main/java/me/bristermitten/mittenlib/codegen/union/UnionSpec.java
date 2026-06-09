package me.bristermitten.mittenlib.codegen.union;

import com.squareup.javapoet.ClassName;
import java.util.List;
import me.bristermitten.mittenlib.codegen.GenericTypeSpec;
import me.bristermitten.mittenlib.codegen.MatchStrategies;
import me.bristermitten.mittenlib.codegen.record.RecordConstructorSpec;

public record UnionSpec(
        ClassName source, ClassName name, MatchStrategies strategy, List<RecordConstructorSpec> constructors)
        implements GenericTypeSpec {}
