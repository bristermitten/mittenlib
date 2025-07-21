# Record Generator

A "polyfill" annotation processor for emulating Java 16 records :)

# Example

```java

@Record
interface TestRecordSpec {
    TestRecordSpec Child1();

    TestRecordSpec Child2(int value);
}
```

will generate a record type called `TestRecord`