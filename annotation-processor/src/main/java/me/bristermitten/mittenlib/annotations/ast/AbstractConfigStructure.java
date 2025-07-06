package me.bristermitten.mittenlib.annotations.ast;

import com.squareup.javapoet.ClassName;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The abstract shape of a config, before proper resolution
 */
public sealed interface AbstractConfigStructure {
    @Contract(pure = true)
    ClassName name();

    /**
     * @return The name of the class this config is enclosed in, if present
     */
    @Contract(pure = true)
    @Nullable ASTParentReference enclosedIn();

    @Contract(pure = true)
    List<AbstractConfigStructure> enclosed();


    @Contract(pure = true)
    List<Property> properties();

    @Contract(pure = true)
    @NotNull
    ConfigTypeSource source();

    @Contract(pure = true)
    @NotNull
    ASTSettings.ConfigASTSettings settings();

    record Intersection(ClassName name,
                        ConfigTypeSource source,
                        ASTSettings.ConfigASTSettings settings,
                        @Nullable ASTParentReference enclosedIn,
                        List<AbstractConfigStructure> enclosed,
                        List<ClassName> roots,
                        List<Property> properties) implements AbstractConfigStructure {
    }

    record Union(
            ClassName name,
            ConfigTypeSource source,
            ASTSettings.ConfigASTSettings settings,
            @Nullable ASTParentReference enclosedIn,
            List<ClassName> parents,
            List<AbstractConfigStructure> alternatives,
            List<Property> properties
    ) implements AbstractConfigStructure {
        @Override
        public List<AbstractConfigStructure> enclosed() {
            return alternatives;
        }
    }

    record Atomic(
            ClassName name,
            ConfigTypeSource source,
            ASTSettings.ConfigASTSettings settings,
            List<AbstractConfigStructure> enclosed,
            @Nullable ASTParentReference enclosedIn,
            List<Property> properties
    ) implements AbstractConfigStructure {
    }

}
