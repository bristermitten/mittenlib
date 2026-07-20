package me.bristermitten.mittenlib.config;

/// Marks a property as transient to the config annotation processor.
/// This is useful for helper methods on interface configs. For example:
/// ```java
/// @Config
/// interface SpawnConfig {
///     String world();
///     int x();
///     int y();
///     int z();
///     default Location spawnPoint() {
///         return new Location(Bukkit.getWorld(world()), x(), y(), z());
///     }
/// }
/// ```
/// From the perspective of the annotation processor, `spawnPoint` is a normal property with a default value.
/// As such, it will try to generate SerDes code for Location, and often fail.
/// In reality, we want it to function as a normal method and be ignored by the processor.
/// This annotation on `spawnPoint` achieves that.
public @interface ConfigTransient {}
