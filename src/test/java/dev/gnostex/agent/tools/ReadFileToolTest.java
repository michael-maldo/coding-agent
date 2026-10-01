package dev.gnostex.agent.tools;

import dev.gnostex.agent.workspace.Workspace;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ReadFileToolTest {

    @TempDir
    Path tempDirectory;

    @Test
    void readsFileInsideWorkspace() throws Exception {

        Path sourceDirectory =
                tempDirectory.resolve("src");

        Files.createDirectories(sourceDirectory);

        Path file =
                sourceDirectory.resolve("Example.java");

        Files.writeString(
                file,
                """
                public class Example {
                    public int value() {
                        return 42;
                    }
                }
                """
        );

        Workspace workspace =
                new Workspace(tempDirectory);

        ReadFileTool tool =
                new ReadFileTool(workspace);

        String result =
                tool.execute("src/Example.java");

        assertTrue(
                result.contains("public class Example")
        );

        assertTrue(
                result.contains("return 42")
        );
    }


    @Test
    void rejectsFileOutsideWorkspace() {

        Workspace workspace =
                new Workspace(tempDirectory);

        ReadFileTool tool =
                new ReadFileTool(workspace);

        assertThrows(
                SecurityException.class,
                () -> tool.execute(
                        "../../../../etc/passwd"
                )
        );
    }


    @Test
    void rejectsMissingFile() {

        Workspace workspace =
                new Workspace(tempDirectory);

        ReadFileTool tool =
                new ReadFileTool(workspace);

        assertThrows(
                IllegalArgumentException.class,
                () -> tool.execute(
                        "does-not-exist.java"
                )
        );
    }


    @Test
    void rejectsDirectoryInsteadOfFile()
            throws Exception {

        Path directory =
                tempDirectory.resolve("src");

        Files.createDirectories(directory);

        Workspace workspace =
                new Workspace(tempDirectory);

        ReadFileTool tool =
                new ReadFileTool(workspace);

        assertThrows(
                IllegalArgumentException.class,
                () -> tool.execute("src")
        );
    }
}
