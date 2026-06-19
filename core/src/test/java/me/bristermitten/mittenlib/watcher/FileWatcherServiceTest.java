package me.bristermitten.mittenlib.watcher;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import me.bristermitten.mittenlib.TestMittenLibConsumer;
import org.junit.jupiter.api.Test;

class FileWatcherServiceTest {

    @Test
    void testWatchingNormalFile() throws IOException, ExecutionException, InterruptedException {
        try (FileSystem fs = Jimfs.newFileSystem(Configuration.forCurrentPlatform())) {
            Path testDir = fs.getPath("/MittenLib");
            Files.createDirectories(testDir);
            Path testFile = testDir.resolve("test.txt");

            var ws = fs.newWatchService();

            FileWatcherService service = new NativeFileWatcherService(
                    () -> ws, new TestMittenLibConsumer(), java.util.logging.Logger.getLogger("test"));

            CompletableFuture<Void> viewed = new CompletableFuture<>();
            service.addWatcher(new FileWatcher(testFile, event -> viewed.complete(null)))
                    .get();

            Files.writeString(testFile, "test");

            assertDoesNotThrow(() -> viewed.get());
        }
    }

    @Test
    void testWatchingSubdirectoryFile() throws IOException, ExecutionException, InterruptedException {
        try (FileSystem fs = Jimfs.newFileSystem(Configuration.forCurrentPlatform())) {
            Path testDir = fs.getPath("/MittenLib/Other/Sub");
            Files.createDirectories(testDir);
            Path testFile = testDir.resolve("test.txt");
            Files.createFile(testFile);

            var ws = fs.newWatchService();

            FileWatcherService service = new NativeFileWatcherService(
                    () -> ws, new TestMittenLibConsumer(), java.util.logging.Logger.getLogger("test"));

            CompletableFuture<Void> viewed = new CompletableFuture<>();
            service.addWatcher(new FileWatcher(testFile, event -> viewed.complete(null)))
                    .get();

            Files.writeString(testFile, "test");

            assertDoesNotThrow(() -> viewed.get());
        }
    }
}
