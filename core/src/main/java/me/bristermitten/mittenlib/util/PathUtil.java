package me.bristermitten.mittenlib.util;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collections;
import java.util.Objects;

public class PathUtil {
    private PathUtil() {
    }

    //Credit https://stackoverflow.com/questions/15713119/java-nio-file-path-for-a-classpath-resource
    public static @NotNull Path resourceToPath(@NotNull URL resource)
            throws IOException,
            URISyntaxException {

        Objects.requireNonNull(resource, "Resource URL cannot be null");
        URI uri = resource.toURI();

        String scheme = uri.getScheme();
        if (scheme.equals("file")) {
            return Paths.get(uri);
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
                    provider.newFileSystem(uri, Collections.emptyMap());
                }
            }
        }
        return Paths.get(uri);
    }
}
