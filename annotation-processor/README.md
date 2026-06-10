# Annotation Processor

This module provides the annotation processor responsible for generating the high-performance configuration and data classes used throughout MittenLib.

## Quick Start (Recommended approach)

The recommended way to define a configuration is using an **Interface**.

```java
import me.bristermitten.mittenlib.config.*;

@Config
@Source("database.yml")
public interface SQLConfig {
    String host();
    String username();
    String password();
    String database();

    default int port() {
        return 3306;
    }

    @Nullable
    String tablePrefix();
}
```

The annotation processor will generate `SQLConfigImpl`, along with its Deserializer, Serializer, and Validator.

## Registration with Guice

The simplest way to register your configurations is using the generated `ConfigLoaderModule`:

```java
public class MyModule extends AbstractModule {
    @Override
    protected void configure() {
        install(new ConfigLoaderModule());
    }
}
```

This will automatically find and register all `@Config` types that have a `@Source` annotation. You can then inject them anywhere:

```java
@Inject
private ConfigProvider<SQLConfig> config;
```

## Documentation

For full technical details, including naming patterns, validation constraints, and advanced Guice integration, please refer to the **[Configuration System Reference](https://bristermitten.github.io/mittenlib/docs/config/)**.

## Installation

```kotlin
dependencies {
    implementation("me.bristermitten:mittenlib-core:VERSION")
    annotationProcessor("me.bristermitten:mittenlib-annotation-processor:VERSION")
}
```

(Note: You only need the processor at compile-time. The generated code only depends on `mittenlib-core`).
