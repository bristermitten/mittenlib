# Record Generator

A "polyfill" annotation processor for emulating Java 16 records and sealed classes, to create convenient discriminated
unions.

# Example

```java
@Record
interface TestRecordSpec {
    TestRecordSpec Child1();

    TestRecordSpec Child2(int value);
}
```

will generate a record type called `TestRecord`