package me.bristermitten.mittenlib.util;

import me.bristermitten.mittenlib.util.lambda.IOFunction;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collections;
import java.util.Objects;

/**
 * Utility class for working with {@link Path}s
 */
public class PathUtil {
    private PathUtil() {
    }

    /**
     * Converts a {@link URL} to a {@link Path}.
     * <a href="https://stackoverflow.com/questions/15713119/java-nio-file-path-for-a-classpath-resource">Credit</a>
     *
     * @deprecated Resource leak, use {@link #resourceToPath(URL, IOFunction)} instead
     */
    @Deprecated
    public static @NotNull Path resourceToPath(@NotNull URL resource) throws IOException,
            URISyntaxException {

        Objects.requireNonNull(resource, "Resource URL cannot be null");
        URI uri = resource.toURI();

        String scheme = uri.getScheme();
        Path path = Paths.get(uri);
        if (scheme.equals("file")) {
            return path;
        }

        if (!scheme.equals("jar")) {
            throw new IllegalArgumentException("Cannot convert to Path: " + uri);
        }

        for (FileSystemProvider provider : FileSystemProvider.installedProviders()) {
            if (provider.getScheme().equalsIgnoreCase("jar")) {
                try {
                    provider.getFileSystem(uri);
                } catch (FileSystemNotFoundException e) {
                    // in this case we need to initialize it first:
                    //noinspection resource i assume this is safe
                    provider.newFileSystem(uri, Collections.emptyMap());
                }
            }
        }
        return path;
    }


    public static @NotNull <T> T resourceToPath(@NotNull URL resource, IOFunction<Path, T> function)
            throws IOException,
            URISyntaxException {

        Objects.requireNonNull(resource, "Resource URL cannot be null");
        URI uri = resource.toURI();

        String scheme = uri.getScheme();
        if (scheme.equals("file")) {
            return function.apply(Paths.get(uri));
        }

        if (!scheme.equals("jar")) {
            throw new IllegalArgumentException("Cannot convert to Path: " + uri);
        }

        for (FileSystemProvider provider : FileSystemProvider.installedProviders()) {
            if (provider.getScheme().equalsIgnoreCase("jar")) {
                try {
                    provider.getFileSystem(uri);
                } catch (FileSystemNotFoundException e) {
                    // in this case we need to initialize it first:
                    try (FileSystem fileSystem = provider.newFileSystem(uri, Collections.emptyMap())) {
                        return function.apply(fileSystem.getPath(uri.getPath()));
                    }
                }
            }
        }
        throw new IllegalStateException("Could not find a FileSystemProvider for " + uri);
    }
}
