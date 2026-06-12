package me.bristermitten.mittenlib.config.paths;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import me.bristermitten.mittenlib.util.Result;

/** A {@link ConfigPathResolver} that resolves config paths from the jar resources. */
public class JarResourcesConfigPathResolver implements ConfigPathResolver {

    @Override
    public Result<Path> getConfigPath(String configFileName) {
        URI uri;
        try {
            URL resource = getClass().getClassLoader().getResource(configFileName);
            if (resource == null) {
                return Result.fail(new RuntimeException("Could not find resource " + configFileName));
            }
            uri = resource.toURI();
            if ("file".equals(uri.getScheme())) {
                return Result.ok(Paths.get(uri));
            }
            FileSystem fileSystem = getFileSystem(uri);
            return Result.ok(fileSystem.getPath(configFileName));
        } catch (URISyntaxException | IOException e) {
            return Result.fail(e);
        }
    }

    private FileSystem getFileSystem(URI uri) throws IOException {
        try {
            return FileSystems.getFileSystem(uri);
        } catch (FileSystemNotFoundException e) {
            Map<String, String> env = new HashMap<>();
            env.put("create", "true");
            return FileSystems.newFileSystem(uri, env);
        }
    }
}
