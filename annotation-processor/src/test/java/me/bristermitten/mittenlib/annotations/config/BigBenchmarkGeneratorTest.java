package me.bristermitten.mittenlib.annotations.config;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import io.toolisticon.cute.Cute;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import me.bristermitten.mittenlib.config.Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BigBenchmarkGeneratorTest {
    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz";

    private Stream<String> alphabetNames;

    @BeforeEach
    void init() {
        alphabetNames = IntStream.rangeClosed(0, Integer.MAX_VALUE)
                .boxed()
                .flatMap(i -> ALPHABET.chars().mapToObj(x -> (char) x).map(x -> x + "" + i));
    }

    @Test
    void generateFullConfigClassName() {
        var builder = TypeSpec.classBuilder(ClassName.get("me.bristermitten.mittenlib.tests", "BenchmarkDTO"))
                .addAnnotation(Config.class);

        alphabetNames
                .limit(250) // Higher than this will generally crash the compiler for varying reasons
                .forEach(name -> builder.addField(TypeName.INT, name));

        TypeSpec build = builder.build();

        Cute.blackBoxTest()
                .given()
                .processor(ConfigProcessor.class)
                .andSourceFile(build.name(), build.toString())
                .whenCompiled()
                .thenExpectThat()
                .compilationSucceeds()
                .executeTest();
    }
}
