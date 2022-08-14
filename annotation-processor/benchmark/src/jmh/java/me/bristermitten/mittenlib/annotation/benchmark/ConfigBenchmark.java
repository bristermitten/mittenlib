package me.bristermitten.mittenlib.annotation.benchmark;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;
import me.bristermitten.mittenlib.config.ConfigModule;
import me.bristermitten.mittenlib.config.Configuration;
import me.bristermitten.mittenlib.config.provider.ConfigProvider;
import me.bristermitten.mittenlib.config.provider.construct.ConfigProviderFactory;
import me.bristermitten.mittenlib.files.FileTypeModule;
import me.bristermitten.mittenlib.files.json.JSONFileType;
import me.bristermitten.mittenlib.files.yaml.YamlFileType;
import org.openjdk.jmh.annotations.*;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class ConfigBenchmark {

    @Benchmark
    public TestData benchmarkMittenLibJson(BenchState state) {
        return state.configProviderJson.get();
    }

    @Benchmark
    public TestDataGson benchmarkGsonJson(BenchState state) {
        return state.gson.fromJson(state.jsonData, TestDataGson.class);
    }

    @Benchmark
    public TestDataGson benchmarkJacksonJson(BenchState state) throws JsonProcessingException {
        return state.jackson.readValue(state.jsonData, TestDataGson.class);

    }

    @Benchmark
    public TestData benchmarkMittenLibYaml(BenchState state) {
        return state.configProviderYaml.get();
    }

    @Benchmark
    public TestDataGson benchmarkGsonYaml(BenchState state) {
        var yaml = state.yaml.load(state.yamlData);
        var tree = state.gson.toJsonTree(yaml);
        return state.gson.fromJson(tree, TestDataGson.class);
    }

    @Benchmark
    public TestDataGson benchmarkSnakeYaml(BenchState state) {
        return state.yaml.loadAs(state.yamlData, TestDataGson.class);
    }


    @State(Scope.Benchmark)
    public static class BenchState {
        public Gson gson = new Gson();
        public Yaml yaml = new Yaml();
        public ObjectMapper jackson = new ObjectMapper();
        public ConfigProvider<TestData> configProviderJson;
        public ConfigProvider<TestData> configProviderYaml;
        private String yamlData;
        private String jsonData;

        @Setup
        public void setup() {
            this.yamlData = getYamlFile();
            this.jsonData = getJSONFile();

            Injector injector = Guice.createInjector(
                    Modules.override(new ConfigModule(Set.of()))
                            .with(new BenchmarkingModule()),
                    new FileTypeModule()
            );
            ConfigProviderFactory configProviderFactory = injector.getInstance(ConfigProviderFactory.class);

            var config = new Configuration<>("data.json", TestData.class, TestData::deserializeTestData);
            var jsonType = injector.getInstance(JSONFileType.class);
            this.configProviderJson = configProviderFactory.createStringReaderProvider(jsonType, jsonData, config);

            var config2 = new Configuration<>("data.yaml", TestData.class, TestData::deserializeTestData);
            var yamlType = injector.getInstance(YamlFileType.class);
            this.configProviderYaml = configProviderFactory.createStringReaderProvider(yamlType, yamlData, config2);
        }

        public String getJSONFile() {
            return load("data.json");
        }

        public String getYamlFile() {
            return load("data.yaml");
        }

        private String load(String fileName) {
            try (var is = getClass()
                    .getClassLoader()
                    .getResourceAsStream(fileName)) {
                return new String(Objects.requireNonNull(is).readAllBytes());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
