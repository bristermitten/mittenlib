package me.bristermitten.mittenlib.annotations.config;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import me.bristermitten.mittenlib.annotations.util.ElementsFinder;
import me.bristermitten.mittenlib.annotations.util.TypesUtil;
import me.bristermitten.mittenlib.util.Strings;

import javax.inject.Inject;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

/**
 * Generates constructors for configuration classes.
 */
public class ConstructorGenerator {
    private final ElementsFinder elementsFinder;
    private final Types types;
    private final MethodNames methodNames;
    private final TypesUtil typesUtil;

    @Inject
    public ConstructorGenerator(
            ElementsFinder elementsFinder,
            Types types,
            MethodNames methodNames,
            TypesUtil typesUtil) {
        this.elementsFinder = elementsFinder;
        this.types = types;
        this.methodNames = methodNames;
        this.typesUtil = typesUtil;
    }

    /**
     * Adds an all-args constructor to the given type spec builder.
     *
     * @param variableElements The variable elements to include in the constructor
     * @param fieldSpecs The field specs to initialize in the constructor
     * @param typeSpecBuilder The type spec builder to add the constructor to
     * @param superclass The superclass of the type, if any
     * @param getConfigClassName A function to get the config class name for a type
     * @param getDTOSuperclass A function to get the superclass of a DTO type
     * @param getFieldAccessorName A function to get the accessor name for a field
     */
    public void addAllArgsConstructor(
            List<VariableElement> variableElements,
            Collection<FieldSpec> fieldSpecs,
            TypeSpec.Builder typeSpecBuilder,
            @Nullable TypeMirror superclass,
            Function<TypeMirror, TypeName> getConfigClassName,
            Function<TypeElement, TypeMirror> getDTOSuperclass,
            Function<VariableElement, String> getFieldAccessorName) {

        final MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC);

        if (superclass != null) {
            var superclassName = getConfigClassName.apply(superclass);
            var superParamName = getSuperFieldName(superclass);
            var parameter = ParameterSpec.builder(superclassName, superParamName)
                    .addModifiers(Modifier.FINAL)
                    .build();
            constructorBuilder.addParameter(parameter);

            var superElements = elementsFinder.getApplicableVariableElements(superclass);
            List<String> collect = superElements.stream()
                    .map(variableElement -> superParamName + "." + getFieldAccessorName.apply(variableElement) + "()")
                    .toList();

            if (getDTOSuperclass.apply((TypeElement) types.asElement(superclass)) != null) {
                var newCollect = new ArrayList<>(collect);
                newCollect.addFirst(superParamName);
                collect = newCollect;
            }

            constructorBuilder.addStatement("super($L)", String.join(", ", collect));

            constructorBuilder.addStatement("this.$L = $L", superParamName, superParamName);
        }

        constructorBuilder.addParameters(variableElements.stream()
                .map(element -> createParameterSpec(element, getConfigClassName))
                .toList());

        fieldSpecs.forEach(field ->
                constructorBuilder.addStatement("this.$N = $N", field, field.name));
        typeSpecBuilder.addMethod(constructorBuilder.build());
    }

    /**
     * Creates a parameter spec for a variable element.
     *
     * @param element The variable element
     * @param getConfigClassName A function to get the config class name for a type
     * @return The parameter spec
     */
    private ParameterSpec createParameterSpec(VariableElement element, Function<TypeMirror, TypeName> getConfigClassName) {
        return ParameterSpec.builder(
                        getConfigClassName.apply(element.asType()),
                        element.getSimpleName().toString()
                ).addModifiers(Modifier.FINAL)
                .build();
    }

    /**
     * Gets the field name for a superclass.
     *
     * @param superClass The superclass
     * @return The field name
     */
    public String getSuperFieldName(TypeMirror superClass) {
        var configName = typesUtil.getConfigClassName(superClass);
        return "parent" + Strings.capitalize(typesUtil.getSimpleName(configName));
    }
}
