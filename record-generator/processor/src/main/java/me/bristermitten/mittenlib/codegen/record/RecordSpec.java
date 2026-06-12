package me.bristermitten.mittenlib.codegen.record;

import com.palantir.javapoet.ClassName;
import me.bristermitten.mittenlib.codegen.GenericTypeSpec;

public record RecordSpec(ClassName source, ClassName name, RecordConstructorSpec constructor)
        implements GenericTypeSpec, RecordSpecLike {}
