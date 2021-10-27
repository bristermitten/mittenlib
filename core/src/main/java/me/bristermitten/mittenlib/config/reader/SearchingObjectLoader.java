package me.bristermitten.mittenlib.config.reader;

import me.bristermitten.mittenlib.files.FileType;
import me.bristermitten.mittenlib.util.Result;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import static me.bristermitten.mittenlib.util.Result.fail;

public class SearchingObjectLoader implements ObjectLoader {
    private final Set<FileType> loaders;

    @Inject
    public SearchingObjectLoader(Set<FileType> loaders) {
        this.loaders = loaders;
    }

    @Override
    public @NotNull Result<Map<String, Object>> load(@NotNull Path source) {
        for (FileType fileType : loaders) {
            if (!fileType.matches(source)) {
                continue;
            }
            return fileType.loader().load(source);
        }
        return fail(new IllegalStateException("Could not find a matching file type for path " + source));
    }
}
