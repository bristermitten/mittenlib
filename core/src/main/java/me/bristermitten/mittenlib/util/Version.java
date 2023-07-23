package me.bristermitten.mittenlib.util;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Minecraft versions "enum"
 */
public class Version implements Comparable<Version> {
    /**
     * Unknown version, probably something very old
     */
    public static final Version UNKNOWN = new Version(0, 0, 0);
    /**
     * Version 1.8.8
     */
    public static final Version VER_1_8_8 = new Version(1, 8, 8);
    /**
     * Version 1.8.9
     */
    public static final Version VER_1_8_9 = new Version(1, 8, 9);
    /**
     * Version 1.9
     */

    public static final Version VER_1_9 = new Version(1, 9, 0);
    /**
     * Version 1.9.1
     */
    public static final Version VER_1_9_1 = new Version(1, 9, 1);
    /**
     * Version 1.9.2
     */

    public static final Version VER_1_9_2 = new Version(1, 9, 2);
    /**
     * Version 1.9.3
     */
    public static final Version VER_1_9_3 = new Version(1, 9, 3);
    /**
     * Version 1.9.4
     */
    public static final Version VER_1_9_4 = new Version(1, 9, 4);
    /**
     * Version 1.10
     */
    public static final Version VER_1_10 = new Version(1, 10, 0);
    /**
     * Version 1.10.1
     */

    public static final Version VER_1_10_1 = new Version(1, 10, 1);
    /**
     * Version 1.10.2
     */

    public static final Version VER_1_10_2 = new Version(1, 10, 2);
    /**
     * Version 1.11
     */

    public static final Version VER_1_11 = new Version(1, 11, 0);
    /**
     * Version 1.11.1
     */

    public static final Version VER_1_11_1 = new Version(1, 11, 1);
    /**
     * Version 1.11.2
     */

    public static final Version VER_1_11_2 = new Version(1, 11, 2);
    /**
     * Version 1.12
     */
    public static final Version VER_1_12 = new Version(1, 12, 0);

    /**
     * Version 1.12.1
     */
    public static final Version VER_1_12_1 = new Version(1, 12, 1);
    /**
     * Version 1.12.2
     */
    public static final Version VER_1_12_2 = new Version(1, 12, 2);
    /**
     * Version 1.13
     */
    public static final Version VER_1_13 = new Version(1, 13, 0);
    /**
     * Version 1.13.1
     */
    public static final Version VER_1_13_1 = new Version(1, 13, 1);
    /**
     * Version 1.13.2
     */
    public static final Version VER_1_13_2 = new Version(1, 13, 2);
    /**
     * Version 1.14
     */

    public static final Version VER_1_14 = new Version(1, 14, 0);
    /**
     * Version 1.14.1
     */
    public static final Version VER_1_14_1 = new Version(1, 14, 1);
    /**
     * Version 1.14.2
     */
    public static final Version VER_1_14_2 = new Version(1, 14, 2);
    /**
     * Version 1.14.3
     */
    public static final Version VER_1_14_3 = new Version(1, 14, 3);
    /**
     * Version 1.14.4
     */
    public static final Version VER_1_14_4 = new Version(1, 14, 4);
    /**
     * Version 1.15
     */
    public static final Version VER_1_15 = new Version(1, 15, 0);
    /**
     * Version 1.15.1
     */
    public static final Version VER_1_15_1 = new Version(1, 15, 1);
    /**
     * Version 1.15.2
     */
    public static final Version VER_1_15_2 = new Version(1, 15, 2);
    /**
     * Version 1.16
     */
    public static final Version VER_1_16 = new Version(1, 16, 0);
    /**
     * Version 1.16.1
     */
    public static final Version VER_1_16_1 = new Version(1, 16, 1);
    /**
     * Version 1.16.2
     */
    public static final Version VER_1_16_2 = new Version(1, 16, 2);
    /**
     * Version 1.16.3
     */
    public static final Version VER_1_16_3 = new Version(1, 16, 3);
    /**
     * Version 1.16.4
     */
    public static final Version VER_1_16_4 = new Version(1, 16, 4);
    /**
     * Version 1.16.5
     */
    public static final Version VER_1_16_5 = new Version(1, 16, 5);
    /**
     * Version 1.17
     */
    public static final Version VER_1_17 = new Version(1, 17, 0);
    /**
     * Version 1.17.1
     */
    public static final Version VER_1_17_1 = new Version(1, 17, 1);
    /**
     * Version 1.18
     */
    public static final Version VER_1_18 = new Version(1, 18, 0);

    /**
     * Version 1.18.1
     */
    public static final Version VER_1_18_1 = new Version(1, 18, 1);

    /**
     * Version 1.18.2
     */
    public static final Version VER_1_18_2 = new Version(1, 18, 2);

    /**
     * Version 1.19
     */
    public static final Version VER_1_19 = new Version(1, 19, 0);

    /**
     * Version 1.19.1
     */
    public static final Version VER_1_19_1 = new Version(1, 19, 1);

    /**
     * Version 1.19.2
     */

    public static final Version VER_1_19_2 = new Version(1, 19, 2);

    /**
     * Version 1.19.3
     */

    public static final Version VER_1_19_3 = new Version(1, 19, 3);

    /**
     * Version 1.19.4
     */

    public static final Version VER_1_19_4 = new Version(1, 19, 4);

    /**
     * Version 1.20
     */

    public static final Version VER_1_20 = new Version(1, 20, 0);

    /**
     * Version 1.20.1
     */
    public static final Version VER_1_20_1 = new Version(1, 20, 1);


    private static final Cached<Version> serverVersion;

    static {
        serverVersion = new Cached<>(() -> {
            try {
                final String version = Bukkit.getBukkitVersion().split("-")[0];
                final String[] parts = version.split("\\.");
                int majorVer = Integer.parseInt(parts[0]);
                int minorVer = Integer.parseInt(parts[1]);
                int patchVer = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
                return new Version(majorVer, minorVer, patchVer);
            } catch (RuntimeException e) {
                e.printStackTrace();
                return UNKNOWN;
            }
        });
    }

    private final int major;
    private final int minor;
    private final int patch;

    public Version(int major, int minor, int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    /**
     * @return the server version, or {@link #UNKNOWN} if it could not be determined
     */
    public static Version getServerVersion() {
        return serverVersion.get();
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public int getPatch() {
        return patch;
    }

    public boolean isNewerThan(@NotNull Version other) {
        return compareTo(other) > 0;
    }

    public boolean isOlderThan(@NotNull Version other) {
        return compareTo(other) < 0;
    }

    @Override
    public int compareTo(@NotNull Version o) {
        if (this.major > o.major) {
            return 1;
        }
        if (this.major < o.major) {
            return -1;
        }
        if (this.minor > o.minor) {
            return 1;
        }
        if (this.minor < o.minor) {
            return -1;
        }
        return Integer.compare(this.patch, o.patch);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Version)) return false;
        Version version = (Version) o;
        return getMajor() == version.getMajor() && getMinor() == version.getMinor() && getPatch() == version.getPatch();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getMajor(), getMinor(), getPatch());
    }
}
