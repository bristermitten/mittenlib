package me.bristermitten.mittenlib.config.reader;

import me.bristermitten.mittenlib.config.tree.DataTree;
import me.bristermitten.mittenlib.files.FileType;
import me.bristermitten.mittenlib.util.Result;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.io.Reader;
import java.nio.file.Path;
import java.util.Set;
import java.util.logging.Logger;

import static me.bristermitten.mittenlib.util.Result.fail;

public class SearchingObjectLoader implements ObjectLoader {
    private final Set<FileType> loaders;

    private final Logger logger;

    @Inject
    public SearchingObjectLoader(Set<FileType> loaders, Logger logger) {
        this.loaders = loaders;
        this.logger = logger;
    }

    @Override
    public @NotNull Result<DataTree> load(@NotNull Path source) {
        for (FileType fileType : loaders) {
            if (!fileType.matches(source)) {
                continue;
            }
            return fileType.loader().load(source);
        }
        return fail(new IllegalStateException("Could not find a matching file type for path " + source));
    }

    @Override
    public @NotNull Result<DataTree> load(@NotNull Reader source) {
        logger.warning(() -> "SearchingObjectLoader used with load(Reader). " +
                "This is not recommended as we can't efficiently determine which Loader to use, and so must try all of them." +
                "Consider using ConfigProviderFactory#createStringReaderProvider(FileType, String, Configuration<T>) to manually specify the file type.");

        for (FileType fileType : loaders) {
            Result<DataTree> res = fileType.loader().load(source); // TODO this won't actually work as the reader is already consumed
            if (res.isSuccess()) {
                return res;
            }
        }
        return fail(new IllegalStateException("Could not find a matching file type for reader " + source));
    }

    @Override
    public @NotNull Result<DataTree> load(@NotNull String source) {
        logger.warning(() -> "SearchingObjectLoader used with load(String). " +
                "This is not recommended as we can't efficiently determine which Loader to use, and so must try all of them." +
                "Consider using ConfigProviderFactory#createStringReaderProvider(FileType, String, Configuration<T>) to manually specify the file type.");


        for (FileType fileType : loaders) {
            Result<DataTree> res = fileType.loader().load(source);
            if (res.isSuccess()) {
                return res;
            }
        }
        return fail(new IllegalStateException("Could not find a matching file type for reader " + source));
    }
}
