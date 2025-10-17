package me.bristermitten.mittenlib.annotations.compile;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import me.bristermitten.mittenlib.annotations.ast.Property;

import javax.inject.Inject;
import javax.lang.model.element.Modifier;
import java.util.List;

/**
 * Generates toString methods for configuration classes.
 * This class creates a standard toString implementation that includes all properties
 * of a configuration class in a readable format.
 */
public class ToStringGenerator {

    @Inject
    public ToStringGenerator() {
    }

    /**
     * Generates a toString method for a configuration class.
     * The generated method returns a string representation of the class in the format:
     * "ClassName{property1=value1, property2=value2, ...}"
     *
     * @param properties The list of properties to include in the toString method
     * @param className  The name of the class for which the toString method is being generated
     * @return A MethodSpec representing the generated toString method
     */
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
