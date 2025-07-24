package me.bristermitten.mittenlib.codegen.record;

import com.squareup.javapoet.TypeName;

import java.util.List;

/**
 * A record with a single constructor
 *
 * @param name   the name of the record
 * @param fields the fields of the record
 */
public record RecordConstructorSpec(
        String name,
        List<RecordFieldSpec> fields
) {
    public record RecordFieldSpec(
            String name,
            TypeName type
    ) {
    }
}
