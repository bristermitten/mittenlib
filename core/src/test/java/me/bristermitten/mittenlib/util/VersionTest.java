package me.bristermitten.mittenlib.util;

import org.bukkit.Bukkit;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VersionTest {

    @Test
    void getServerVersion() {
        try (var mockedBukkit = Mockito.mockStatic(Bukkit.class)) {
            mockedBukkit.when(Bukkit::getBukkitVersion).thenReturn("1.21.1-R0.1-SNAPSHOT");

            Version version = Version.getServerVersion();
            assertEquals(1, version.getMajor());
            assertEquals(21, version.getMinor());
            assertEquals(1, version.getPatch());
        }
    }
}