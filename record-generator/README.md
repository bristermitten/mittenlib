# Record Generator

A "polyfill" annotation processor for emulating Java 16 records and sealed classes, to create convenient discriminated
unions.

# Records Example

```java
import me.bristermitten.mittenlib.codegen.Record;
@Record
interface TestRecordSpec {
    TestRecordSpec create(String a, int b);
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

# Union Example

We can also emulate sealed classes / discriminated unions with the `@Union` annotation:

```java
import me.bristermitten.mittenlib.codegen.Union;
@Union
interface TestUnionSpec {
    TestUnionSpec Child1();

    TestUnionSpec Child2(int value);
}
```

This will generate a "sealed" class named `TestUnion` with the following methods:

```java
static TestUnion Child1();

static TestUnion Child2(int value);

Optional<Child1> asChild1();

Optional<Child2> asChild2();

void match(
        Consumer<Child1> child1Case,
        Consumer<Child2> child2Case
);

<T> T matchAs(
        Function<Child1, T> child1Case,
        Function<Child2, T> child2Case
);
```

The `Child1` and `Child2` classes are generated as subclasses of `TestUnion`, generated as if they were annotated with
`@Record`

The class is "sealed" in the sense that the constructor is private, preventing implementation outside the generated
class.
Furthermore, the constructor also performs a check to ensure that the instance is one of the defined subclasses,
preventing instantiation hacks. Of course, something like `Unsafe.allocateInstance` can still be used to bypass this ;)

## Options

Customisation options are fairly minimal at the moment.

### Class Names

By default, the generated class names are derived by taking the interface name and stripping the `Spec` suffix.
It is recommended to follow this naming convention where possible, however, the `@Record` and `@Union` annotations
also support a `name` parameter to specify a custom class name.

```java
import me.bristermitten.mittenlib.codegen.Record;
@Record(name = "CustomName")
interface NoSpecNeeded {
    // blah
}
```

would generate a class named `CustomName`.

### Match Strategy

By default, _nominal_ pattern matching is used, meaning that the `match` and `matchAs` methods use the subclass
types as parameters. However, you can also use _structural_ pattern matching using the `@MatchStrategy` annotation:

```java
import me.bristermitten.mittenlib.codegen.Union;
import me.bristermitten.mittenlib.codegen.MatchStrategy;
import me.bristermitten.mittenlib.codegen.MatchStrategies;
@Union
@MatchStrategy(MatchStrategies.STRUCTURAL)
interface TestUnionSpec {
    TestUnionSpec Child1();

    TestUnionSpec Child2(int value);
}
```

With this option set, the `match` and `matchAs` methods will destructure the child types into their fields, like so:

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

## Usage

This module requires 2 dependencies to be added:

- The API module, with the artefact ID `me.bristermitten.mittenlib.record-generator-api`
- The annotation processor, with the artefact ID `me.bristermitten.mittenlib.record-generator-processor`

With Gradle Kotlin, this can be done like so:

```kotlin
dependencies {
	implementation("me.bristermitten.mittenlib:record-generator-api:VERSION")
	annotationProcessor("me.bristermitten.mittenlib:record-generator-processor:VERSION")
}
```

