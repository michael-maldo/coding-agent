package dev.gnostex.agent.workspace;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class WorkspaceTest {

    @TempDir
    Path tempDirectory;

    @Test
    void resolvesFileInsideWorkspace() throws Exception {

        Path sourceDirectory =
                tempDirectory.resolve("src");

        Files.createDirectories(sourceDirectory);

        Path file =
                sourceDirectory.resolve("Example.java");

        Files.writeString(
                file,
                "public class Example {}"
        );

        Workspace workspace =
                new Workspace(tempDirectory);

        Path resolved =
                workspace.resolve(
                        "src/Example.java"
                );

        assertEquals(
                file.toAbsolutePath().normalize(),
                resolved
        );
    }

    @Test
    void rejectsPathOutsideWorkspace() {

        Workspace workspace =
                new Workspace(tempDirectory);

        assertThrows(
                SecurityException.class,
                () -> workspace.resolve(
                        "../../../../etc/passwd"
                )
        );
    }

    @Test
    void rejectsMissingWorkspaceDirectory() {

        Path missingDirectory =
                tempDirectory.resolve(
                        "does-not-exist"
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> new Workspace(
                        missingDirectory
                )
        );
    }

    @Test
void rejectsSymlinkOutsideWorkspace()
        throws Exception {

    Path outsideDirectory =
            Files.createTempDirectory(
                    "outside-workspace"
            );

    try {

        Path outsideFile =
                outsideDirectory.resolve(
                        "secret.txt"
                );

        Files.writeString(
                outsideFile,
                "secret information"
        );

        Path link =
                tempDirectory.resolve(
                        "secret-link"
                );

        Files.createSymbolicLink(
                link,
                outsideFile
        );

        Workspace workspace =
                new Workspace(tempDirectory);

        assertThrows(
                SecurityException.class,
                () -> workspace.resolve(
                        "secret-link"
                )
        );

    } finally {

        Files.walk(outsideDirectory)
                .sorted(
                        java.util.Comparator.reverseOrder()
                )
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (Exception ignored) {
                    }
                });
    }
}
}
