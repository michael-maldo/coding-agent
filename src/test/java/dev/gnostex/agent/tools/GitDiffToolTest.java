package dev.gnostex.agent.tools;

import dev.gnostex.agent.workspace.Workspace;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class GitDiffToolTest {

    @TempDir
    Path tempDirectory;

    private Path calculatorFile;
    private GitDiffTool tool;

    @BeforeEach
    void setUp() throws Exception {

        runGit("init");

        /*
         * Configure identity only inside this
         * temporary repository.
         */
        runGit(
                "config",
                "user.email",
                "test@example.com"
        );

        runGit(
                "config",
                "user.name",
                "Test User"
        );

        Path sourceDirectory =
                tempDirectory.resolve(
                        "src/main/java/practice"
                );

        Files.createDirectories(
                sourceDirectory
        );

        calculatorFile =
                sourceDirectory.resolve(
                        "Calculator.java"
                );

        Files.writeString(
                calculatorFile,
                """
                package practice;

                public class Calculator {

                    public int add(int a, int b) {
                        return a + b;
                    }
                }
                """
        );

        runGit("add", ".");

        runGit(
                "commit",
                "-m",
                "Initial commit"
        );

        Workspace workspace =
                new Workspace(tempDirectory);

        tool =
                new GitDiffTool(workspace);
    }


    @Test
    void reportsNoChangesWhenRepositoryIsClean()
            throws Exception {

        String result =
                tool.execute("");

        assertEquals(
                "No changes.",
                result
        );
    }


    @Test
    void showsModifiedSourceCode()
            throws Exception {

        Files.writeString(
                calculatorFile,
                """
                package practice;

                public class Calculator {

                    public int add(int a, int b) {
                        return a + b;
                    }

                    public int subtract(int a, int b) {
                        return a - b;
                    }
                }
                """
        );

        String result =
                tool.execute("");

        assertTrue(
                result.contains(
                        "Calculator.java"
                )
        );

        assertTrue(
                result.contains(
                        "public int subtract"
                )
        );

        assertTrue(
                result.contains(
                        "return a - b;"
                )
        );
    }


    private void runGit(String... arguments)
            throws Exception {

        String[] command =
                new String[
                        arguments.length + 1
                ];

        command[0] = "git";

        System.arraycopy(
                arguments,
                0,
                command,
                1,
                arguments.length
        );

        Process process =
                new ProcessBuilder(command)
                        .directory(
                                tempDirectory.toFile()
                        )
                        .redirectErrorStream(true)
                        .start();

        String output =
                new String(
                        process.getInputStream()
                                .readAllBytes()
                );

        int exitCode =
                process.waitFor();

        if (exitCode != 0) {

            fail(
                    "Git command failed: "
                            + String.join(
                                    " ",
                                    command
                            )
                            + "\n"
                            + output
            );
        }
    }
}
