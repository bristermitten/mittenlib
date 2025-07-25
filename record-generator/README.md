# Record Generator

A "polyfill" annotation processor for emulating Java 16 records and sealed classes, to create convenient discriminated
unions.

# Records Example

```java
@Record
interface TestRecordSpec {
    TestRecordSpec create(String a, int b);
}
```

This will generate an immutable data class named `TestRecord` with the following methods:

```java
String a();
int b();

static TestRecord create(String a, int b);
```

We can also emulate sealed classes with the `@Union` annotation:

```java
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
        Runnable child1Case,
        Consumer<Integer> child2Case
);

<T> T matchAs(
        Supplier<T> child1Case,
        Function<Integer, T> child2Case
);
```

