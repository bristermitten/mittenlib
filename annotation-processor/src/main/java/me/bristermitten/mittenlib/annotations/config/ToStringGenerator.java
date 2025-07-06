package me.bristermitten.mittenlib.annotations.config;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import me.bristermitten.mittenlib.annotations.ast.Property;

import javax.inject.Inject;
import javax.lang.model.element.Modifier;
import java.util.List;

public class ToStringGenerator {

    private final MethodNames methodNames;

    @Inject public ToStringGenerator(MethodNames methodNames) {
        this.methodNames = methodNames;
    }

    public MethodSpec generateToString(List<Property> properties,
                                       ClassName className) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("toString")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class);
        var code = CodeBlock.builder();
        code.add("return \"$T{\"", className);

        for (int i = 0; i < properties.size(); i++) {
            Property fieldSpec = properties.get(i);
            code.add(" + \"$L=\" + $N ", fieldSpec.name(), fieldSpec.name());
            if (i != properties.size() - 1) {
                code.add("+ \",\"");
            }
        }
        code.add("+ \"}\"");
        builder.addStatement(code.build());
        return builder.build();
    }
}
