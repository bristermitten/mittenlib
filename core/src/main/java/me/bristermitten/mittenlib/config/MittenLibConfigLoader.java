package me.bristermitten.mittenlib.config;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.util.Modules;
import java.util.Collections;
import java.util.Set;
import me.bristermitten.mittenlib.MittenLib;
import org.jetbrains.annotations.ApiStatus;

/**
 * Base class for modules that load configurations.
 *
 * <p>This class is NOT a Guice {@link Module}. It is a high-level configuration descriptor used by
 * {@link MittenLib} to register configurations and set up the necessary infrastructure.
 */
public abstract class MittenLibConfigLoader {

    /**
     * Get the configurations loaded by this module.
     *
     * @return the set of configurations
     */
    public Set<Configuration<?>> getConfigurations() {
        return Collections.emptySet();
    }

    /**
     * Configure the configurations for this module. This is called internally when building the Guice
     * module.
     *
     * @param binder the Guice binder being configured
     */
    protected abstract void configure(Binder binder);

    /**
     * Get this configuration loader as a Guice {@link Module}.
     *
     * <p><b>Warning:</b> Using this directly bypasses MittenLib's infrastructure setup. It should
     * primarily be used for testing.
     *
     * @return the Guice module
     */
    @ApiStatus.Internal
    public abstract Module asModule();

    /**
     * Get this configuration loader as a Guice {@link Module}, including default infrastructure.
     *
     * <p>This is useful for tests that need a complete configuration setup without using {@link
     * MittenLib}.
     *
     * @return the Guice module with infrastructure
     */
    @ApiStatus.Internal
    public final Module asModuleWithInfrastructure() {
        return Modules.combine(asModule(), new ConfigInfrastructureModule());
    }
}
