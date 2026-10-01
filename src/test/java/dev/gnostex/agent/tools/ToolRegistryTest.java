package dev.gnostex.agent.tools;
import dev.gnostex.agent.core.ToolCall;

import dev.gnostex.agent.workspace.Workspace;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ToolRegistryTest {

    @TempDir
    Path tempDirectory;

    private ToolRegistry registry;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() throws Exception {

        /*
         * Create a small fake repository.
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
         * SearchCodeTool uses git grep.
         */
        runGit("init");
        runGit("add", ".");

        Workspace workspace =
                new Workspace(tempDirectory);

        registry =
                new ToolRegistry();

        registry.register(
                new ReadFileTool(workspace)
        );

        registry.register(
                new SearchCodeTool(workspace)
        );

        mapper =
                new ObjectMapper();
    }


    @Test
    void executesReadFileTool()
            throws Exception {

        ToolCall call =
                new ToolCall(
                        "read_file",
                        mapper.readTree(
                                """
                                {
                                  "path":
                                  "src/main/java/practice/Calculator.java"
                                }
                                """
                        )
                );

        String result =
                registry.execute(call);

        assertTrue(
                result.contains(
                        "public class Calculator"
                )
        );

        assertTrue(
                result.contains(
                        "public int add"
                )
        );
    }


    @Test
    void executesSearchCodeTool()
            throws Exception {

        ToolCall call =
                new ToolCall(
                        "search_code",
                        mapper.readTree(
                                """
                                {
                                  "query": "Calculator"
                                }
                                """
                        )
                );

        String result =
                registry.execute(call);

        assertTrue(
                result.contains(
                        "Calculator.java"
                )
        );

        assertTrue(
                result.contains(
                        "public class Calculator"
                )
        );
    }


    @Test
    void rejectsUnknownTool()
            throws Exception {

        ToolCall call =
                new ToolCall(
                        "delete_everything",
                        mapper.readTree(
                                """
                                {}
                                """
                        )
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> registry.execute(call)
                );

        assertTrue(
                exception.getMessage()
                        .contains("Unknown tool")
        );
    }


    @Test
    void rejectsMissingRequiredArgument()
            throws Exception {

        ToolCall call =
                new ToolCall(
                        "read_file",
                        mapper.readTree(
                                """
                                {}
                                """
                        )
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> registry.execute(call)
                );

        assertTrue(
                exception.getMessage()
                        .contains(
                                "Missing tool argument"
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
