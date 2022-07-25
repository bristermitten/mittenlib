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
import org.openjdk.jmh.infra.Blackhole;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class ConfigBenchmark {

    @Benchmark
    public void benchmarkMittenLibJson(BenchState state, Blackhole bh) {
        var res = state.configProviderJson.get();
        bh.consume(res);
    }

    @Benchmark
    public void benchmarkGsonJson(BenchState state, Blackhole bh) {
        var res = state.gson.fromJson(state.jsonData, TestDataGson.class);
        bh.consume(res);
    }

    @Benchmark
    public void benchmarkJacksonJson(BenchState state, Blackhole bh) throws JsonProcessingException {
        var res = state.jackson.readValue(state.jsonData, TestDataGson.class);
        bh.consume(res);
    }

    @Benchmark
    public void benchmarkMittenLibYaml(BenchState state, Blackhole bh) {
        var res = state.configProviderYaml.get();
        bh.consume(res);
    }

    @Benchmark
    public void benchmarkGsonYaml(BenchState state, Blackhole bh) {
        var yaml = state.yaml.load(state.yamlData);
        var tree = state.gson.toJsonTree(yaml);
        var res = state.gson.fromJson(tree, TestDataGson.class);
        bh.consume(res);
    }

    @Benchmark
    public void benchmarkSnakeYaml(BenchState state, Blackhole bh) {
        var res = state.yaml.loadAs(state.yamlData, TestDataGson.class);
        bh.consume(res);
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
