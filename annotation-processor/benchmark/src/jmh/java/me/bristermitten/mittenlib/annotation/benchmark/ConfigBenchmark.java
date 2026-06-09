package me.bristermitten.mittenlib.annotation.benchmark;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import me.bristermitten.mittenlib.config.DeserializationContext;
import me.bristermitten.mittenlib.config.DeserializationFunction;
import me.bristermitten.mittenlib.config.reader.ObjectLoader;
import me.bristermitten.mittenlib.config.tree.DataTree;
import me.bristermitten.mittenlib.files.FileTypeModule;
import me.bristermitten.mittenlib.files.json.JSONFileType;
import me.bristermitten.mittenlib.files.yaml.YamlFileType;
import org.openjdk.jmh.annotations.*;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class ConfigBenchmark {

    @Benchmark
    public TestData benchmarkMittenLibFullJson(BenchState state) {
        return state.jsonLoader
                .load(state.jsonData)
                .map(tree -> new DeserializationContext(state.mittenMapper, tree))
                .flatMap(state.deserializer::apply)
                .getOrThrow();
    }

    @Benchmark
    public TestDataGson benchmarkGsonFullJson(BenchState state) {
        return state.gson.fromJson(state.jsonData, TestDataGson.class);
    }

    @Benchmark
    public TestDataGson benchmarkJacksonFullJson(BenchState state) throws JsonProcessingException {
        return state.jackson.readValue(state.jsonData, TestDataGson.class);
    }

    @Benchmark
    public TestData benchmarkMittenLibMappingJson(BenchState state) {
        return state.deserializer
                .apply(new DeserializationContext(state.mittenMapper, state.jsonTree))
                .getOrThrow();
    }

    @Benchmark
    public TestDataGson benchmarkGsonMappingJson(BenchState state) {
        return state.gson.fromJson(state.gsonTree, TestDataGson.class);
    }

    @Benchmark
    public TestDataGson benchmarkJacksonMappingJson(BenchState state) throws JsonProcessingException {
        return state.jackson.treeToValue(state.jacksonTree, TestDataGson.class);
    }

    @State(Scope.Benchmark)
    public static class BenchState {
        public final Gson gson = new Gson();
        public final Yaml yaml = new Yaml();
        public final ObjectMapper jackson = new ObjectMapper();

        public me.bristermitten.mittenlib.config.reader.ObjectMapper mittenMapper;
        public ObjectLoader jsonLoader;
        public ObjectLoader yamlLoader;
        public DeserializationFunction<TestData> deserializer;

        public String yamlData;
        public String jsonData;

        public DataTree jsonTree;
        public JsonElement gsonTree;
        public JsonNode jacksonTree;

        @Setup
        public void setup() throws JsonProcessingException {
            this.yamlData = getYamlFile();
            this.jsonData = getJSONFile();

            Injector injector = Guice.createInjector(
                    new ConfigLoaderModule().asModuleWithInfrastructure(),
                    new BenchmarkingModule(),
                    new FileTypeModule());

            this.mittenMapper = injector.getInstance(me.bristermitten.mittenlib.config.reader.ObjectMapper.class);
            this.deserializer = injector.getInstance(Key.get(new TypeLiteral<>() {}));

            this.jsonLoader = injector.getInstance(JSONFileType.class).loader();
            this.yamlLoader = injector.getInstance(YamlFileType.class).loader();

            // Pre-parsed trees for mapping benchmarks
            this.jsonTree = jsonLoader.load(jsonData).getOrThrow();
            this.gsonTree = gson.toJsonTree(gson.fromJson(jsonData, Object.class));
            this.jacksonTree = jackson.valueToTree(jackson.readValue(jsonData, Object.class));
        }

        public String getJSONFile() {
            return load("data.json");
        }

        public String getYamlFile() {
            return load("data.yaml");
        }

        private String load(String fileName) {
            try (var is = getClass().getClassLoader().getResourceAsStream(fileName)) {
                return new String(Objects.requireNonNull(is).readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
