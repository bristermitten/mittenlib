package me.bristermitten.mittenlib.config.writer;

import me.bristermitten.mittenlib.config.tree.DataTree;
import me.bristermitten.mittenlib.files.FileType;
import me.bristermitten.mittenlib.util.Result;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.io.Writer;
import java.nio.file.Path;
import java.util.Set;
import java.util.logging.Logger;

import static me.bristermitten.mittenlib.util.Result.fail;

public class SearchingObjectWriter implements ObjectWriter {
    private final Set<FileType> fileTypes;

    private final Logger logger;

    @Inject
    public SearchingObjectWriter(Set<FileType> fileTypes, Logger logger) {
        this.fileTypes = fileTypes;
        this.logger = logger;
    }


    @Override
    public @NotNull Result<Void> write(@NotNull DataTree tree, @NotNull Path path) {
        for (FileType fileType : fileTypes) {
            if (!fileType.matches(path)) {
                continue;
            }
            return fileType.writer().write(tree, path);
        }
        return fail(new IllegalStateException("Could not find a matching file type for path " + path));
    }

    @Override
    public @NotNull Result<Void> write(@NotNull DataTree tree, @NotNull Writer output) {
        logger.warning(() -> "SearchingObjectWriter used with write(DataTree, Writer). " +
                "This is not recommended as we can't efficiently determine which Writer to use, and so must try all of them." +
                "Consider using a specific ObjectWriter for better performance.");

        for (FileType fileType : fileTypes) {
            Result<Void> res = fileType.writer().write(tree, output); // TODO this won't actually work as the writer is already consumed
            if (res.isSuccess()) {
                return res;
            }
        }
        return fail(new IllegalStateException("Could not find a matching file type for writer " + output));
    }

    @Override
    public @NotNull Result<String> write(@NotNull DataTree tree) {
        logger.warning(() -> "SearchingObjectWriter used with write(DataTree). " +
                "This is not recommended as we can't efficiently determine which Writer to use, and so must try all of them." +
                "Consider using a specific ObjectWriter for better performance.");

        for (FileType fileType : fileTypes) {
            Result<String> res = fileType.writer().write(tree);
            if (res.isSuccess()) {
                return res;
            }
        }
        return fail(new IllegalStateException("Could not find a matching file type for DataTree " + tree));
    }
}
