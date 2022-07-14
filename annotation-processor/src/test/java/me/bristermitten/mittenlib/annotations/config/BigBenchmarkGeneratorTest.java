package me.bristermitten.mittenlib.annotations.config;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import me.bristermitten.mittenlib.config.Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

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
        var javaObject = JavaFileObjects.forSourceString(build.name, build.toString());

        Compilation compilation = javac()
                .withProcessors(new ConfigProcessor())
                .compile(javaObject);

        assertThat(compilation).succeededWithoutWarnings();
    }
}
