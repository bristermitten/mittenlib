package me.bristermitten.mittenlib.records;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;

import java.util.List;

public record RecordSpec(
        ClassName source,
        ClassName name,
        List<RecordConstructorSpec> constructors
) {

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
}
