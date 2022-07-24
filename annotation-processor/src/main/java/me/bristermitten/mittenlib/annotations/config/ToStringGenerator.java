package me.bristermitten.mittenlib.annotations.config;

import com.squareup.javapoet.*;

import javax.lang.model.element.Modifier;

public class ToStringGenerator {

    public MethodSpec generateToString(TypeSpec.Builder typeSpecBuilder,
                                       ClassName className) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("toString")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class);
        var code = CodeBlock.builder();
        code.add("return \"$T{\"", className);
        for (FieldSpec fieldSpec : typeSpecBuilder.fieldSpecs) {
            code.add(" + \"$L=\" + $N + \",\"", fieldSpec.name, fieldSpec.name);
        }
        code.add(" + \"}\"");
        builder.addStatement(code.build());
        return builder.build();
    }
}
