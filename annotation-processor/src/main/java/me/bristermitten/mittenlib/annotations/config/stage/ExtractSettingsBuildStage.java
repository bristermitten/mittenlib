package me.bristermitten.mittenlib.annotations.config.stage;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeSpec;
import me.bristermitten.mittenlib.annotations.config.ConfigClassBuildSettings;
import me.bristermitten.mittenlib.annotations.config.ConfigurationClassNameGenerator;

import javax.inject.Inject;
import javax.lang.model.element.TypeElement;

public class ExtractSettingsBuildStage implements BuildStage<Void, ConfigClassBuildSettings> {
    private final ConfigurationClassNameGenerator classNameGenerator;

    @Inject
    ExtractSettingsBuildStage(ConfigurationClassNameGenerator classNameGenerator) {
        this.classNameGenerator = classNameGenerator;
    }

    @Override
    public String name() {
        return "extract-settings";
    }

    @Override
    public ConfigClassBuildSettings apply(TypeElement generateFrom, TypeSpec.Builder builder, Void input) {
        final ClassName className =
                classNameGenerator.generateConfigurationClassName(generateFrom)
                        .orElseThrow(() -> new IllegalArgumentException("Cannot determine name for @Config class " + classType.getQualifiedName()));


    }
}
