package me.bristermitten.mittenlib.util;

import me.bristermitten.mittenlib.util.lambda.SafeSupplier;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class ResultTest {

    @Test
    void ok() {
        Result<String> result = Result.ok("Hello");
        assertTrue(result.isSuccess());
        assertFalse(result.isFailure());
        assertEquals("Hello", result.getOrThrow());
    }

    @Test
    void fail() {
        Result<String> result = Result.fail(new IllegalArgumentException());
        assertFalse(result.isSuccess());
        assertTrue(result.isFailure());
        assertThrows(IllegalArgumentException.class, result::getOrThrow);
    }

    @Test
    void runCatching() {
        Result<String> result = Result.runCatching(() -> "Hello");
        assertTrue(result.isSuccess());
        assertFalse(result.isFailure());
        assertEquals("Hello", result.getOrThrow());
    }

    @Test
    void runCatchingWithFail() {
        Result<String> result = Result.runCatching(() -> {
            throw new IllegalArgumentException();
        });
        assertFalse(result.isSuccess());
        assertTrue(result.isFailure());
        assertThrows(IllegalArgumentException.class, result::getOrThrow);
        assertEquals(IllegalArgumentException.class, result.error().map(Exception::getClass).orElse(null));
    }

    @Test
    void execCatching() {
        Result<Unit> result = Result.execCatching(() -> {

        });
        assertTrue(result.isSuccess());
        assertFalse(result.isFailure());
        assertEquals(Unit.UNIT, result.getOrThrow());
    }

    @Test
    void execCatchingWithFail() {
        Result<Unit> result = Result.execCatching(() -> {
            throw new IllegalArgumentException();
        });
        assertFalse(result.isSuccess());
        assertTrue(result.isFailure());
        assertThrows(IllegalArgumentException.class, result::getOrThrow);
        assertEquals(IllegalArgumentException.class, result.error().map(Exception::getClass).orElse(null));
    }

    @Test
    void computeCatching() {
        Result<String> result = Result.computeCatching(() -> Result.ok("Hello"));
        assertTrue(result.isSuccess());
        assertFalse(result.isFailure());
        assertEquals("Hello", result.getOrThrow());
    }

    @Test
    void computeCatchingWithFail() {
        Result<String> result = Result.computeCatching(() ->
                Result.runCatching(() -> {
                    throw new IllegalArgumentException();
                }));
        assertFalse(result.isSuccess());
        assertTrue(result.isFailure());
        assertThrows(IllegalArgumentException.class, result::getOrThrow);
        assertEquals(IllegalArgumentException.class, result.error().map(Exception::getClass).orElse(null));
    }

    @Test
    void tryWithResources() {
        AtomicBoolean closed = new AtomicBoolean(false);
        AutoCloseable i = () -> closed.set(true);
        Result<String> result = Result.tryWithResources(i, ignored -> Result.ok("Hello"));
        assertTrue(result.isSuccess());
        assertFalse(result.isFailure());
        assertEquals("Hello", result.getOrThrow());
        assertTrue(closed.get());
    }

    @Test
    void tryWithResourcesWithFail() {
        AtomicBoolean closed = new AtomicBoolean(false);
        AutoCloseable i = () -> closed.set(true);
        Result<String> result = Result.tryWithResources(i, ignored -> {
            throw new RuntimeException();
        });
        assertFalse(result.isSuccess());
        assertTrue(result.isFailure());
        assertThrows(RuntimeException.class, result::getOrThrow);
        assertTrue(closed.get());
    }

    @Test
    void tryWithResourcesWithFailInSupplier() {
        AtomicBoolean closed = new AtomicBoolean(false);
        AutoCloseable i = () -> closed.set(true);
        SafeSupplier<AutoCloseable> supplier = () -> {
            throw new RuntimeException();
        };
        Result<String> result = Result.tryWithResources(supplier, ignored -> Result.ok("Hello"));
        assertFalse(result.isSuccess());
        assertTrue(result.isFailure());
        assertThrows(RuntimeException.class, result::getOrThrow);
        assertFalse(closed.get()); // should not be closed, as the supplier failed
    }

    @Test
    void tryWithResourcesWithFailInBoth() {
        AtomicBoolean closed = new AtomicBoolean(false);
        AutoCloseable i = () -> closed.set(true);
        SafeSupplier<AutoCloseable> supplier = () -> {
            throw new RuntimeException("1");
        };
        Result<String> result = Result.tryWithResources(supplier, ignored -> {
            throw new RuntimeException("2");
        });
        assertFalse(result.isSuccess());
        assertTrue(result.isFailure());
        assertThrows(RuntimeException.class, result::getOrThrow);
        assertFalse(closed.get()); // should not be closed, as the supplier failed
        // the supplier exception should be the one that is thrown
        assertEquals("1", result.error().map(Throwable::getMessage).orElse(null));
    }

    @Test
    void sequence() {
        var results = Result.sequence(
                List.of(
                        Result.ok("Hello"),
                        Result.ok("World")
                )
        );
        assertTrue(results.isSuccess());
        assertFalse(results.isFailure());
        assertEquals(List.of("Hello", "World"), results.getOrThrow());
    }

    @Test
    void toOptional() {
        var result = Result.ok("Hello");
        assertTrue(result.toOptional().isPresent());
        assertEquals("Hello", result.toOptional().get());

        var result2 = Result.fail(new IllegalArgumentException());
        assertTrue(result2.toOptional().isEmpty());
    }

    @Test
    void error() {
        var result = Result.ok("Hello");
        assertTrue(result.error().isEmpty());

        var result2 = Result.fail(new IllegalArgumentException());
        assertTrue(result2.error().isPresent());
        assertEquals(IllegalArgumentException.class, result2.error().get().getClass());
    }

    @Test
    void map() {
        var result = Result.ok("Hello");
        var result2 = result.map(String::length);
        assertTrue(result2.isSuccess());
        assertFalse(result2.isFailure());
        assertEquals(5, result2.getOrThrow());
    }

    @Test
    void mapError() {
        Result<String> result = Result.fail(new IllegalArgumentException());
        var result2 = result.map(String::length);
        assertFalse(result2.isSuccess());
        assertTrue(result2.isFailure());
        assertThrows(IllegalArgumentException.class, result::getOrThrow);
        assertEquals(IllegalArgumentException.class, result.error().map(Exception::getClass).orElse(null));
    }

    @Test
    void orElse() {
        Result<String> result = Result.ok("Hello");
        assertEquals(Result.ok("Hello"), result.orElse(() -> Result.ok("World")));
        assertEquals(Result.ok("Hello"), result.orElse(() -> Result.fail(new IllegalArgumentException())));

        Result<String> result2 = Result.fail(new IllegalArgumentException());
        assertEquals(Result.ok("World"), result2.orElse(() -> Result.ok("World")));
        IllegalArgumentException ex = new IllegalArgumentException();
        assertEquals(Result.fail(ex), result2.orElse(() -> Result.fail(ex)));
    }

    @Test
    void flatMap() {
        Result<String> result = Result.ok("Hello");
        var result2 = result.flatMap(s -> Result.ok(s.length()));
        assertTrue(result2.isSuccess());
        assertFalse(result2.isFailure());
        assertEquals(5, result2.getOrThrow());

        Result<String> result3 = Result.fail(new IllegalArgumentException());
        var result4 = result3.flatMap(s -> Result.ok(s.length()));
        assertFalse(result4.isSuccess());
        assertTrue(result4.isFailure());
        assertThrows(IllegalArgumentException.class, result3::getOrThrow);

        Result<String> result5 = Result.ok("Hello");
        var result6 = result5.flatMap(s -> Result.fail(new IllegalArgumentException()));
        assertFalse(result6.isSuccess());
        assertTrue(result6.isFailure());
        assertThrows(IllegalArgumentException.class, result6::getOrThrow);
    }

    @Test
    void getOrThrow() {
        Result<String> result = Result.ok("Hello");
        assertEquals("Hello", result.getOrThrow());

        Result<String> result2 = Result.fail(new IllegalArgumentException());
        assertThrows(IllegalArgumentException.class, result2::getOrThrow);
    }

    @Test
    void handle() {
        Result<String> result = Result.ok("Hello");
        var handled = result.handle(Function.identity(), e -> "World");
        assertEquals("Hello", handled);

        Result<String> result2 = Result.fail(new IllegalArgumentException());
        var handled2 = result2.handle(Function.identity(), e -> "World");
        assertEquals("World", handled2);
    }

    @Test
    void recover() {
        Result<String> result = Result.ok("Hello");
        var recovered = result.recover(e -> "World");
        assertEquals("Hello", recovered);

        Result<String> result2 = Result.fail(new IllegalArgumentException());
        var recovered2 = result2.recover(e -> "World");
        assertEquals("World", recovered2);
    }

    @Test
    void isSuccess() {
        Result<String> result = Result.ok("Hello");
        assertTrue(result.isSuccess());

        Result<String> result2 = Result.fail(new IllegalArgumentException());
        assertFalse(result2.isSuccess());
    }

    @Test
    void isFailure() {
        Result<String> result = Result.ok("Hello");
        assertFalse(result.isFailure());

        Result<String> result2 = Result.fail(new IllegalArgumentException());
        assertTrue(result2.isFailure());
    }
}
