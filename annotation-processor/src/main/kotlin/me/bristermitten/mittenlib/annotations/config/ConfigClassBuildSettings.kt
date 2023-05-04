package me.bristermitten.mittenlib.annotations.config;

import com.squareup.javapoet.ClassName;

public record ConfigClassBuildSettings(
        ClassName generatedClassName,
        boolean generateRecord
) {
}
