package me.bristermitten.mittenlib.annotations.config

import com.google.testing.compile.Compilation
import com.google.testing.compile.CompilationSubject.assertThat as assertCompilation
import com.google.testing.compile.Compiler.javac
import com.google.testing.compile.JavaFileObjects
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class DTOSuperclassTransformationTest extends AnyFunSuite with Matchers {

  test("testSimpleSuperClassConfig") {
    val compilation: Compilation = javac()
      .withProcessors(new ConfigProcessor())
      .compile(
        JavaFileObjects.forSourceString(
          "me.bristermitten.mittenlib.tests.SuperclassConfigDTO",
          """
          |package me.bristermitten.mittenlib.tests;
          |
          |import java.util.Map;
          |
          |import me.bristermitten.mittenlib.config.*;
          |import me.bristermitten.mittenlib.config.names.*;
          |
          |@NamingPattern(value = me.bristermitten.mittenlib.config.names.NamingPatterns.LOWER_KEBAB_CASE)
          |@Source(value = "lang.yml")
          |@Config(requireDynamicInitialization = false)
          |public class SuperclassConfigDTO {
          |    Child1DTO child1;
          |    Child2DTO child2;
          |
          |    @Config
          |    static class Child1DTO {
          |        int a;
          |    }
          |
          |    @Config
          |    static class Child2DTO extends Child1DTO {
          |        int b;
          |    }
          |}
          |""".stripMargin
        )
      )

    assertCompilation(compilation).succeeded()
    assertCompilation(compilation)
      .generatedSourceFile("me.bristermitten.mittenlib.tests.SuperclassConfig")
      .isNotNull()
  }

  test("testSuperSuperClassConfig") {
    val compilation: Compilation = javac()
      .withProcessors(new ConfigProcessor())
      .compile(
        JavaFileObjects.forSourceString(
          "me.bristermitten.mittenlib.tests.SuperclassConfigDTO",
          """
          |package me.bristermitten.mittenlib.tests;
          |
          |import java.util.Map;
          |
          |import me.bristermitten.mittenlib.config.*;
          |import me.bristermitten.mittenlib.config.names.*;
          |
          |@NamingPattern(value = me.bristermitten.mittenlib.config.names.NamingPatterns.LOWER_KEBAB_CASE)
          |@Source(value = "lang.yml")
          |@Config(requireDynamicInitialization = false)
          |public class SuperclassConfigDTO {
          |    public Child1DTO child1;
          |    public Child2DTO child2;
          |    public Child3DTO child3;
          |    public Child4DTO child4;
          |
          |    @Config
          |    public static class Child1DTO {
          |        int a;
          |    }
          |
          |    @Config
          |    public static class Child2DTO extends Child1DTO {
          |        int b;
          |    }
          |
          |    @Config
          |    public static class Child3DTO extends Child2DTO {
          |        int c;
          |    }
          |    @Config
          |    public static class Child4DTO extends Child3DTO {
          |        int d;
          |    }
          |}
          |""".stripMargin
        )
      )

    assertCompilation(compilation).succeeded()
    assertCompilation(compilation)
      .generatedSourceFile("me.bristermitten.mittenlib.tests.SuperclassConfig")
      .isNotNull()
  }

  test("testMoreAdvancedClassConfig") {
    val compilation: Compilation = javac()
      .withProcessors(new ConfigProcessor())
      .compile(
        JavaFileObjects.forSourceString(
          "me.bristermitten.mittenlib.tests.ShopConfigDTO",
          """
            |package me.bristermitten.mittenlib.tests;
            |
            |import java.util.Map;
            |
            |import me.bristermitten.mittenlib.config.*;
            |import me.bristermitten.mittenlib.config.generate.GenerateToString;
            |import me.bristermitten.mittenlib.config.names.*;
            |import org.jspecify.annotations.Nullable;
            |
            |@Config
            |@NamingPattern(NamingPatterns.LOWER_KEBAB_CASE)
            |@GenerateToString
            |public class ShopConfigDTO {
            |    String title;
            |    int rows;
            |    Map<String, ShopItemConfigDTO> items;
            |
            |
            |    @Config
            |    static class ShopItemConfigDTO extends ItemConfigDTO {
            |        int medianStock;
            |        @Nullable Integer slot;
            |    }
            |}""".stripMargin
        ),
        JavaFileObjects.forSourceString(
          "me.bristermitten.mittenlib.tests.ItemConfigDTO",
          """
            |package me.bristermitten.mittenlib.tests;
            |
            |import java.util.List;
            |import java.util.Map;
            |
            |import me.bristermitten.mittenlib.config.*;
            |import me.bristermitten.mittenlib.config.extension.UseObjectMapperSerialization;
            |import me.bristermitten.mittenlib.config.names.*;
            |import org.bukkit.Material;
            |import org.bukkit.enchantments.Enchantment;
            |import org.bukkit.inventory.ItemFlag;
            |import org.jspecify.annotations.Nullable;
            |
            |@Config
            |@NamingPattern(NamingPatterns.LOWER_KEBAB_CASE)
            |public class ItemConfigDTO {
            |    Material type;
            |    @Nullable String name;
            |    @Nullable List<String> lore;
            |
            |    @Nullable String player;
            |
            |    boolean glow = false;
            |
            |    @Nullable String dyeColor;
            |
            |    @Nullable List<ItemFlag> flags;
            |
            |    @Nullable List<EnchantmentDTO> enchantments;
            |
            |    boolean unbreakable = false;
            |
            |    @Config
            |    static class EnchantmentDTO {
            |        @UseObjectMapperSerialization Enchantment type;
            |        int level = 1;
            |    }
            |}
            |""".stripMargin
        )
      )

    assertCompilation(compilation).succeeded()
    assertCompilation(compilation)
      .generatedSourceFile("me.bristermitten.mittenlib.tests.ShopConfig")
      .isNotNull()

    assertCompilation(compilation)
      .generatedSourceFile("me.bristermitten.mittenlib.tests.ShopConfig")
      .contentsAsUtf8String()
      .contains("super(parent")
  }
}
