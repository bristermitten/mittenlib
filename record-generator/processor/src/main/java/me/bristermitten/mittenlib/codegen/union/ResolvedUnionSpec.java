package me.bristermitten.mittenlib.codegen.union;

import com.squareup.javapoet.ClassName;
import java.util.List;
import me.bristermitten.mittenlib.codegen.GenericTypeSpec;
import me.bristermitten.mittenlib.codegen.MatchStrategies;

public record ResolvedUnionSpec(
        ClassName source, ClassName name, MatchStrategies strategy, List<ResolvedUnionConstructor> constructors)
        implements GenericTypeSpec {}
