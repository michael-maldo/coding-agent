package dev.gnostex.agent.tools;

import dev.gnostex.agent.workspace.Workspace;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SearchCodeToolTest {

    @TempDir
    Path tempDirectory;

    private Workspace workspace;
    private SearchCodeTool tool;

    @BeforeEach
    void setUp() throws Exception {

        /*
         * SearchCodeTool uses git grep, so our temporary
         * workspace must be a Git repository.
         */
        runGit("init");

        /*
         * Create some source code.
         */
        Path sourceDirectory =
                tempDirectory.resolve(
                        "src/main/java/practice"
                );

        Files.createDirectories(
                sourceDirectory
        );

        Files.writeString(
                sourceDirectory.resolve(
                        "Calculator.java"
                ),
                """
                package practice;

                public class Calculator {

                    public int add(int a, int b) {
                        return a + b;
                    }
                }
                """
        );

        /*
         * git grep searches tracked files.
         */
        runGit("add", ".");

        workspace =
                new Workspace(tempDirectory);

        tool =
                new SearchCodeTool(workspace);
    }


    @Test
    void findsClassInRepository()
            throws Exception {

        String result =
                tool.execute("Calculator");

        assertTrue(
                result.contains(
                        "src/main/java/practice/Calculator.java"
                )
        );

        assertTrue(
                result.contains(
                        "public class Calculator"
                )
        );
    }


    @Test
    void findsMethodInRepository()
            throws Exception {

        String result =
                tool.execute("add");

        assertTrue(
                result.contains(
                        "Calculator.java"
                )
        );

        assertTrue(
                result.contains(
                        "public int add"
                )
        );
    }


    @Test
    void returnsNoMatchesWhenSymbolDoesNotExist()
            throws Exception {

        String result =
                tool.execute(
                        "SomethingThatDefinitelyDoesNotExist"
                );

        assertEquals(
                "No matches found.",
                result
        );
    }


    /*
     * Helper used only by the tests.
     */
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

        int exitCode =
                process.waitFor();

        if (exitCode != 0) {

            String output =
                    new String(
                            process.getInputStream()
                                    .readAllBytes()
                    );

            fail(
                    "Git command failed: "
                            + String.join(" ", command)
                            + "\n"
                            + output
            );
        }
    }
}
