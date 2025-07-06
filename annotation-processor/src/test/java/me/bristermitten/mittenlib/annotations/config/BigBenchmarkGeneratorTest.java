package me.bristermitten.mittenlib.annotations.config;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import io.toolisticon.cute.Cute;
import me.bristermitten.mittenlib.config.Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;
import java.util.stream.Stream;

class BigBenchmarkGeneratorTest {
    private static final String ALPHABET =
            "abcdefghijklmnopqrstuvwxyz";

    private Stream<String> alphabetNames;

    @BeforeEach
    void init() {
        alphabetNames = IntStream.rangeClosed(0, Integer.MAX_VALUE)
                .boxed()
                .flatMap(i -> ALPHABET.chars()
                        .mapToObj(x -> (char) x)
                        .map(x -> x + "" + i));
    }

    @Test
    void generateFullConfigClassName() {
        var builder = TypeSpec.classBuilder(ClassName.get("me.bristermitten.mittenlib.tests", "BenchmarkDTO"))
                .addAnnotation(Config.class);

        alphabetNames
                .limit(254) // Higher than this will generally crash the compiler for varying reasons
                .forEach(name -> builder.addField(TypeName.INT, name));

        TypeSpec build = builder.build();

        Cute.blackBoxTest()
                .given()
                .processor(ConfigProcessor.class)
                .andSourceFile(build.name, build.toString())
                .whenCompiled()
                .thenExpectThat()
                .compilationSucceeds()
                .executeTest();
    }
}