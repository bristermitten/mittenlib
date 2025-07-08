package me.bristermitten.mittenlib.files.yaml;

import me.bristermitten.mittenlib.collections.Maps;
import me.bristermitten.mittenlib.config.tree.DataTree;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import static org.assertj.core.api.Assertions.assertThat;

class YamlObjectLoaderTest {

    private YamlObjectLoader loader;

    @BeforeEach
    void setUp() {
        Yaml yaml = new Yaml();
        loader = new YamlObjectLoader(yaml);
    }

    @AfterEach
    void tearDown() {
        loader = null;
    }

    @Test
    void load() {
        DataTree load = loader.load("123")
                .getOrThrow();

        assertThat(load)
                .isEqualTo(new DataTree.DataTreeLiteral.DataTreeLiteralInt(123));

        DataTree load2 = loader.load("123abc: 123\n" +
                                     "someObjectMap:\n" +
                                     "  { a: 1, b: 1 }: 2.5")
                .getOrThrow();

        assertThat(load2)
                .isNotNull()
                .isEqualTo(new DataTree.DataTreeMap(
                        Maps.of(
                                new DataTree.DataTreeLiteral.DataTreeLiteralString("123abc"),
                                new DataTree.DataTreeLiteral.DataTreeLiteralInt(123),
                                new DataTree.DataTreeLiteral.DataTreeLiteralString("someObjectMap"),
                                new DataTree.DataTreeMap(
                                        Maps.of(
                                                new DataTree.DataTreeMap(
                                                        Maps.of(
                                                                new DataTree.DataTreeLiteral.DataTreeLiteralString("a"),
                                                                new DataTree.DataTreeLiteral.DataTreeLiteralInt(1),
                                                                new DataTree.DataTreeLiteral.DataTreeLiteralString("b"),
                                                                new DataTree.DataTreeLiteral.DataTreeLiteralInt(1)
                                                        )
                                                ),
                                                new DataTree.DataTreeLiteral.DataTreeLiteralFloat(2.5)
                                        )
                                )
                        )
                ));

        assertThat(load2.get("123abc"))
                .isEqualTo(new DataTree.DataTreeLiteral.DataTreeLiteralInt(123));
    }
}