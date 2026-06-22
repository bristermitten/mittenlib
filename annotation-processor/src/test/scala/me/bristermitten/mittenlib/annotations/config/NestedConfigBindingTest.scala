package me.bristermitten.mittenlib.annotations.config

import com.google.testing.compile.Compilation
import com.google.testing.compile.CompilationSubject.assertThat as assertCompilation
import com.google.testing.compile.Compiler.javac
import com.google.testing.compile.JavaFileObjects
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class NestedConfigBindingTest extends AnyFunSuite with Matchers {

  test("testNestedConfigBindingsAreGenerated") {
    val compilation: Compilation = javac()
      .withProcessors(new ConfigProcessor())
      .compile(
        JavaFileObjects.forSourceString(
          "me.bristermitten.mittenlib.tests.RootConfigDTO",
          """
          |package me.bristermitten.mittenlib.tests;
          |
          |import me.bristermitten.mittenlib.config.Config;
          |import me.bristermitten.mittenlib.config.Source;
          |
          |@Source("config.yml")
          |@Config(requireDynamicInitialization = false)
          |public class RootConfigDTO {
          |    public ChildConfigDTO child;
          |
          |    @Config
          |    public static class ChildConfigDTO {
          |        public int i;
          |        public GrandChildConfigDTO grandChild;
          |
          |        @Config
          |        public static class GrandChildConfigDTO {
          |            public String s;
          |        }
          |    }
          |}
          |""".stripMargin
        )
      )

    assertCompilation(compilation).succeeded()

    val module = compilation.generatedSourceFile(
      "me.bristermitten.mittenlib.tests.ConfigLoaderModule"
    )
    module.isPresent shouldBe true

    val content = module.get().getCharContent(true).toString
    content.contains(
      "public RootConfig.ChildConfig provideChildConfig(RootConfig parent)"
    ) shouldBe true
    content.contains("return parent.child();") shouldBe true
    content.contains(
      "public RootConfig.ChildConfig.GrandChildConfig provideGrandChildConfig("
    ) shouldBe true
    content.contains("return parent.grandChild();") shouldBe true
  }

  test("testNestedInterfaceConfigBindingsAreGenerated") {
    val compilation: Compilation = javac()
      .withProcessors(new ConfigProcessor())
      .compile(
        JavaFileObjects.forSourceString(
          "me.bristermitten.mittenlib.tests.RootInterfaceConfig",
          """
          |package me.bristermitten.mittenlib.tests;
          |
          |import me.bristermitten.mittenlib.config.Config;
          |import me.bristermitten.mittenlib.config.Source;
          |
          |@Source("config.yml")
          |@Config(requireDynamicInitialization = false)
          |public interface RootInterfaceConfig {
          |    ChildInterface child();
          |
          |    @Config
          |    interface ChildInterface {
          |        int i();
          |    }
          |}
          |""".stripMargin
        )
      )

    assertCompilation(compilation).succeeded()

    val module = compilation.generatedSourceFile(
      "me.bristermitten.mittenlib.tests.ConfigLoaderModule"
    )
    module.isPresent shouldBe true

    val content = module.get().getCharContent(true).toString
    content.contains(
      "public RootInterfaceConfig.ChildInterface provideChildInterface(RootInterfaceConfig parent)"
    ) shouldBe true
    content.contains("return parent.child();") shouldBe true
  }

  test("testAmbiguousNestedConfigBindingsAreNotGenerated") {
    val compilation: Compilation = javac()
      .withProcessors(new ConfigProcessor())
      .compile(
        JavaFileObjects.forSourceString(
          "me.bristermitten.mittenlib.tests.AmbiguousConfigDTO",
          """
          |package me.bristermitten.mittenlib.tests;
          |
          |import me.bristermitten.mittenlib.config.Config;
          |import me.bristermitten.mittenlib.config.Source;
          |
          |@Source("config.yml")
          |@Config(requireDynamicInitialization = false)
          |public class AmbiguousConfigDTO {
          |    public ChildConfigDTO first;
          |    public ChildConfigDTO second;
          |
          |    @Config
          |    public static class ChildConfigDTO {
          |        public int i;
          |    }
          |}
          |""".stripMargin
        )
      )

    assertCompilation(compilation).succeeded()

    val module = compilation.generatedSourceFile(
      "me.bristermitten.mittenlib.tests.ConfigLoaderModule"
    )
    module.isPresent shouldBe true

    val content = module.get().getCharContent(true).toString
    content.contains("provideChildConfig") shouldBe false
  }

  test("testAmbiguousNestedConfigBindingsWithOverrideAreGenerated") {
    val compilation: Compilation = javac()
      .withProcessors(new ConfigProcessor())
      .compile(
        JavaFileObjects.forSourceString(
          "me.bristermitten.mittenlib.tests.OverrideAmbiguousConfigDTO",
          """
          |package me.bristermitten.mittenlib.tests;
          |
          |import me.bristermitten.mittenlib.config.Config;
          |import me.bristermitten.mittenlib.config.Source;
          |import me.bristermitten.mittenlib.config.BindProperty;
          |
          |@Source("config.yml")
          |@Config(requireDynamicInitialization = false)
          |public class OverrideAmbiguousConfigDTO {
          |    @BindProperty
          |    public ChildConfigDTO first;
          |    public ChildConfigDTO second;
          |
          |    @Config
          |    public static class ChildConfigDTO {
          |        public int i;
          |    }
          |}
          |""".stripMargin
        )
      )

    assertCompilation(compilation).succeeded()

    val module = compilation.generatedSourceFile(
      "me.bristermitten.mittenlib.tests.ConfigLoaderModule"
    )
    module.isPresent shouldBe true

    val content = module.get().getCharContent(true).toString
    content.contains(
      "public OverrideAmbiguousConfig.ChildConfig provideChildConfig(OverrideAmbiguousConfig parent)"
    ) shouldBe true
    content.contains("return parent.first();") shouldBe true
  }

  test("testMultipleBindPropertyAnnotationsFails") {
    val compilation: Compilation = javac()
      .withProcessors(new ConfigProcessor())
      .compile(
        JavaFileObjects.forSourceString(
          "me.bristermitten.mittenlib.tests.DoubleOverrideConfigDTO",
          """
          |package me.bristermitten.mittenlib.tests;
          |
          |import me.bristermitten.mittenlib.config.Config;
          |import me.bristermitten.mittenlib.config.Source;
          |import me.bristermitten.mittenlib.config.BindProperty;
          |
          |@Source("config.yml")
          |@Config(requireDynamicInitialization = false)
          |public class DoubleOverrideConfigDTO {
          |    @BindProperty
          |    public ChildConfigDTO first;
          |    @BindProperty
          |    public ChildConfigDTO second;
          |
          |    @Config
          |    public static class ChildConfigDTO {
          |        public int i;
          |    }
          |}
          |""".stripMargin
        )
      )

    assertCompilation(compilation).failed()
    assertCompilation(compilation)
      .hadErrorContaining(
        "Multiple properties of type ChildConfig are marked with @BindProperty"
      )
  }
}
