# Record Generator

The `record-generator` is a specialized annotation processor that emulates the behavior of Java 16 `record` classes and `sealed` classes on older Java versions.

It allows you to define simple interfaces that are automatically expanded into full-featured, immutable data classes or discriminated unions at compile-time.

## Features

- **Record Emulation**: Immutable data classes with getters, `equals`, `hashCode`, `toString`, and `withX` copy methods.
- **Sealed Class Emulation**: Type-safe discriminated unions with functional pattern matching.
- **Zero Runtime Overhead**: All code is generated at compile-time with no runtime dependencies.

## Documentation

For full technical details, examples, and customization options, please refer to the **[Record Generator Documentation](https://bristermitten.github.io/mittenlib/docs/modules/record-generator)**.

## Installation

```kotlin
dependencies {
    compileOnly("me.bristermitten:mittenlib-record-generator-api:VERSION")
    annotationProcessor("me.bristermitten:mittenlib-record-generator-processor:VERSION")
}
```
