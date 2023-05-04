package me.bristermitten.mittenlib.annotations.config

import com.google.testing.compile.Compilation
import com.google.testing.compile.CompilationSubject
import com.google.testing.compile.Compiler
import com.google.testing.compile.JavaFileObjects
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import me.bristermitten.mittenlib.config.Config
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.stream.IntStream
import java.util.stream.Stream

internal class BigBenchmarkGeneratorTest {
    private lateinit var alphabetNames: Stream<String>

    @BeforeEach
    fun init() {
        alphabetNames = IntStream.rangeClosed(0, Int.MAX_VALUE)
            .boxed()
            .flatMap { i: Int ->
                ALPHABET.chars()
                    .mapToObj { x: Int -> x.toChar() }
                    .map { x: Char -> x.toString() + "" + i }
            }
    }

    @Test
    fun generateFullConfigClassName() {
        val builder = TypeSpec.classBuilder(ClassName.get("me.bristermitten.mittenlib.tests", "BenchmarkDTO"))
            .addAnnotation(Config::class.java)
        alphabetNames
            .limit(254) // Higher than this will generally crash the compiler for varying reasons
            .forEach { name: String? -> builder.addField(TypeName.INT, name) }
        val build: TypeSpec = builder.build()
        val javaObject = JavaFileObjects.forSourceString(build.name, build.toString())
        val compilation: Compilation = Compiler.javac()
            .withProcessors(ConfigProcessor())
            .compile(javaObject)
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()
    }

    companion object {
        private const val ALPHABET = "abcdefghijklmnopqrstuvwxyz"
    }
}
