package me.bristermitten.mittenlib;

import static org.junit.jupiter.api.Assertions.assertEquals;

import be.seeseemelk.mockbukkit.MockBukkit;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.multibindings.Multibinder;
import org.bukkit.event.Listener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
        MockBukkit.unload();
    }

    @Test
    void testGuiceInjection() {
        Injector build = MittenLib.withDefaults(plugin)
                .addModules(new AbstractModule() {
                    @Override
                    protected void configure() {
                        Multibinder.newSetBinder(binder(), Listener.class);
                    }
                })
                .setup();

        assertEquals(
                plugin.getName(), build.getInstance(MittenLibConsumer.class).getName());
    }
}
