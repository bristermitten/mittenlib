package me.bristermitten.mittenlib;

import com.google.inject.Injector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MittenLibGuiceTest {
    private TestMittenLib plugin;

    @BeforeEach
    public void setUp() {

        MockBukkit.mock();
        plugin = MockBukkit.load(TestMittenLib.class);
    }

    @AfterEach
    public void tearDown() {
        // Stop the mock server
        MockBukkit.unmock();
    }


    @Test
    void testGuiceInjection() {
        Injector build = MittenLib.withDefaults(plugin)
                .build();

        assertEquals(plugin.getName(), build.getInstance(MittenLibConsumer.class).getName());
    }
}
