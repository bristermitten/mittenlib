# Record Generator

The `record-generator` is a specialized annotation processor that emulates the behavior of Java 16 `record` classes and `sealed` classes on older Java versions.

**Runtime Dependencies:**
* None (Annotation Processor)

## Why use this?

Many older Minecraft versions (like 1.8.8) limit plugins to Java 8. This means you miss out on modern Java features like Records, which are incredibly useful for clean Data Transfer Objects.

By using this module, you can define a simple interface annotated with the generator's annotations, and MittenLib will generate a boilerplate-free "record-like" class for you to use in your Java 8 compatible plugin.

## Installation

```kotlin
dependencies {
    compileOnly("me.bristermitten:mittenlib-record-generator-api:VERSION")
    annotationProcessor("me.bristermitten:mittenlib-record-generator-processor:VERSION")
}
```

*For complete usage instructions, refer to the [Record Generator README](https://github.com/BristerMitten/mittenlib/tree/master/record-generator).*
