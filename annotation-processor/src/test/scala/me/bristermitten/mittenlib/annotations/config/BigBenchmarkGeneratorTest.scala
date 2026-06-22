package me.bristermitten.mittenlib.annotations.config

import com.palantir.javapoet.ClassName
import com.palantir.javapoet.TypeName
import com.palantir.javapoet.TypeSpec
import io.toolisticon.cute.Cute
import java.util.stream.IntStream
import java.util.stream.Stream
import me.bristermitten.mittenlib.config.Config
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatest.BeforeAndAfterEach

class BigBenchmarkGeneratorTest
    extends AnyFunSuite
    with Matchers
    with BeforeAndAfterEach {
  private val ALPHABET = "abcdefghijklmnopqrstuvwxyz"
  private var alphabetNames: Stream[String] = _

  override def beforeEach(): Unit = {
    alphabetNames = IntStream
      .rangeClosed(0, Integer.MAX_VALUE)
      .boxed()
      .flatMap(i => ALPHABET.chars().mapToObj(x => x.toChar).map(x => s"$x$i"))
  }

  test("generateFullConfigClassName") {
    val builder = TypeSpec
      .classBuilder(
        ClassName.get("me.bristermitten.mittenlib.tests", "BenchmarkDTO")
      )
      .addAnnotation(classOf[Config])

    alphabetNames
      .limit(250)
      .forEach(name => builder.addField(TypeName.INT, name))

    val build = builder.build()

    Cute
      .blackBoxTest()
      .`given`()
      .processor(classOf[ConfigProcessor])
      .andSourceFile(build.name, build.toString)
      .whenCompiled()
      .thenExpectThat()
      .compilationSucceeds()
      .executeTest()
  }
}
