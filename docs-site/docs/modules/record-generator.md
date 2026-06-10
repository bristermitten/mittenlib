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

## Features

### Records Example

```java
import me.bristermitten.mittenlib.codegen.RecordSpec;

@RecordSpec
interface TestRecordSpec {
    String a();

    int b();
}
```

This will generate an immutable data class named `TestRecord` with the following methods:

```java
public final class TestRecord {
    public TestRecord(String a, int b);

    public static TestRecord create(String a, int b);

    public String a();

    public int b();

    public TestRecord withA(String a);

    public TestRecord withB(int b);
}
```

Additionally, the generated class will have standard implementations of `equals`, `hashCode`, and `toString`.

### Union Example

We can also emulate sealed classes / discriminated unions with the `@UnionSpec` annotation:

```java
import me.bristermitten.mittenlib.codegen.UnionSpec;

@UnionSpec
interface TestUnionSpec {
    TestUnionSpec Child1();

    TestUnionSpec Child2(int value);
}
```

This will generate a "sealed" class named `TestUnion` with the following methods:

```java
// Static Factory Methods
static TestUnion Child1();

static TestUnion Child2(int value);

// Safe downcasting
Optional<Child1> asChild1();

Optional<Child2> asChild2();

// Pattern Matching
void match(
        Consumer<Child1> child1Case,
        Consumer<Child2> child2Case
);

<T> T matchAs(
        Function<Child1, T> child1Case,
        Function<Child2, T> child2Case
);
```

The `Child1` and `Child2` classes are generated as subclasses of `TestUnion`, generated as if they were annotated with `@RecordSpec`.

The class is "sealed" in the sense that the constructor is private, preventing implementation outside the generated class. Furthermore, the constructor also performs a check to ensure that the instance is one of the defined subclasses.

## Customization Options

### Class Names

By default, the generated class names are derived by taking the interface name and stripping the `Spec` suffix. It is recommended to follow this naming convention where possible; however, the `@RecordSpec` and `@UnionSpec` annotations also support a `name` parameter to specify a custom class name.

```java
@RecordSpec(name = "CustomName")
interface NoSpecNeeded {
    // ...
}
```

### Match Strategy

By default, _nominal_ pattern matching is used, meaning that the `match` and `matchAs` methods use the subclass types as parameters. However, you can also use _structural_ pattern matching using the `@MatchStrategy` annotation:

```java
import me.bristermitten.mittenlib.codegen.UnionSpec;
import me.bristermitten.mittenlib.codegen.MatchStrategy;
import me.bristermitten.mittenlib.codegen.MatchStrategies;

@UnionSpec
@MatchStrategy(MatchStrategies.STRUCTURAL)
interface TestUnionSpec {
    TestUnionSpec Child1();

    TestUnionSpec Child2(int value);
}
```

With this option set, the `match` and `matchAs` methods will destructure the child types into their fields:

```java
void match(
        Runnable child1Case,
        Consumer<Integer> child2Case
);

<T> T matchAs(
        Supplier<T> child1Case,
        Function<Integer, T> child2Case
);
```
