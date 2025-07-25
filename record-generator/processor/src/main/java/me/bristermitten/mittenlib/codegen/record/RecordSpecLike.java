package me.bristermitten.mittenlib.codegen.record;

import com.squareup.javapoet.ClassName;
import me.bristermitten.mittenlib.codegen.GenericTypeSpec;

public interface RecordSpecLike extends GenericTypeSpec {
    ClassName source();

    RecordConstructorSpec constructor();
}
