package me.bristermitten.mittenlib.config.reader;

import com.google.gson.Gson;
import me.bristermitten.mittenlib.collections.Maps;
import me.bristermitten.mittenlib.config.tree.DataTree;
import me.bristermitten.mittenlib.files.json.GsonObjectLoader;
import me.bristermitten.mittenlib.files.json.GsonObjectWriter;
import me.bristermitten.mittenlib.files.json.JSONFileType;
import me.bristermitten.mittenlib.files.yaml.YamlFileType;
import me.bristermitten.mittenlib.files.yaml.YamlObjectLoader;
import me.bristermitten.mittenlib.files.yaml.YamlObjectWriter;
import me.bristermitten.mittenlib.util.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.util.Set;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;

public class SearchingObjectLoaderTest {
    private SearchingObjectLoader searchingObjectLoader;

    @BeforeEach
    void setUp() {
        var gson = new Gson();
        var yaml = new Yaml();

        searchingObjectLoader = new SearchingObjectLoader(
                Set.of(new JSONFileType(new GsonObjectLoader(gson), new GsonObjectWriter(gson)),
                        new YamlFileType(new YamlObjectLoader(yaml), new YamlObjectWriter(yaml))), Logger.getLogger("SearchingObjectLoader")
        );
    }

    @Test
    void loadJSON() {
        Result<DataTree> load = searchingObjectLoader.load("""
                {"hello": "world"}
                """);

        assertThat(load)
                .isNotNull()
                .extracting(Result::isSuccess)
                .isEqualTo(true);

        assertThat(load.getOrThrow())
                .isEqualTo(new DataTree.DataTreeMap(
                        Maps.of(new DataTree.DataTreeLiteral.DataTreeLiteralString("hello"),
                                new DataTree.DataTreeLiteral.DataTreeLiteralString("world")
                        )
                ));
    }

    @Test
    void loadYAML() {
        Result<DataTree> load = searchingObjectLoader.load("""
                hello: world
                """);

        assertThat(load)
                .isNotNull()
                .extracting(Result::isSuccess)
                .isEqualTo(true);

        assertThat(load.getOrThrow())
                .isEqualTo(DataTree.map(
                        Maps.of(DataTree.string("hello"),
                                DataTree.string("world")
                        )
                ));
    }
}