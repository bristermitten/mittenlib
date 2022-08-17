package me.bristermitten.mittenlib;

import com.google.inject.AbstractModule;
import org.bukkit.plugin.Plugin;

public class TestMittenLib extends MittenLib<Plugin> {
    public TestMittenLib() {
        super(null);

        addModule(
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(MittenLibConsumer.class).toInstance(new TestMittenLibConsumer());
                    }
                }
        );
    }


}
