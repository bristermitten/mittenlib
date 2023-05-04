package me.bristermitten.mittenlib.annotations.config.stage;

import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.TypeElement;

/**
 * A Stage in the process of building a config class
 */
public interface BuildStage<I, O> {
    String name();

    O apply(TypeElement generateFrom, TypeSpec.Builder builder, I input);


}
