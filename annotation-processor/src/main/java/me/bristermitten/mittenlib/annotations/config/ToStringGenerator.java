package me.bristermitten.mittenlib.annotations.config;

import com.squareup.javapoet.*;

import javax.lang.model.element.Modifier;
import java.util.List;

public class ToStringGenerator {

    public MethodSpec generateToString(TypeSpec.Builder typeSpecBuilder,
                                       ClassName className) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("toString")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class);
        var code = CodeBlock.builder();
        code.add("return \"$T{\"", className);
        List<FieldSpec> fieldSpecs = typeSpecBuilder.fieldSpecs;
        for (int i = 0; i < fieldSpecs.size(); i++) {
            FieldSpec fieldSpec = fieldSpecs.get(i);
            code.add(" + \"$L=\" + $N ", fieldSpec.name, fieldSpec.name);
            if (i != fieldSpecs.size() - 1) {
                code.add("+ \",\"");
            }
        }
        code.add("+ \"}\"");
        builder.addStatement(code.build());
        return builder.build();
    }
}
