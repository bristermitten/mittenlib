package me.bristermitten.mittenlib.config.paths;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class JarResourcesConfigPathResolver implements ConfigPathResolver {
    @Override
    public Path getConfigPath(String configFileName) {
        URI uri;
        try {
            URL resource = getClass().getClassLoader().getResource(configFileName);
            if (resource == null) {
                throw new RuntimeException("Could not find resource " + configFileName);
            }
            uri = resource.toURI();
            return getSystem(uri).getPath(configFileName);
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private FileSystem getSystem(URI uri) throws IOException {
        try {
            return FileSystems.getFileSystem(uri);
        } catch (FileSystemNotFoundException e) {
            Map<String, String> env = new HashMap<>();
            env.put("create", "true");
            return FileSystems.newFileSystem(uri, env);
        }
    }
}
