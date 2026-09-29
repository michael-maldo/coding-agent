package dev.gnostex.agent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ReplaceTextToolTest {

    @TempDir
    Path tempDirectory;

    @Test
    void replacesUniqueText()
            throws Exception {

        Path file =
                tempDirectory.resolve(
                        "Calculator.java"
                );

        Files.writeString(
                file,
                """
                public class Calculator {

                    public int add(int a, int b) {
                        return a + b;
                    }
                }
                """
        );

        Workspace workspace =
                new Workspace(tempDirectory);

        ReplaceTextTool tool =
                new ReplaceTextTool(workspace);

        String result =
                tool.replace(
                        "Calculator.java",
                        """
                            public int add(int a, int b) {
                                return a + b;
                            }
                        """,
                        """
                            public int add(int a, int b) {
                                return a + b;
                            }

                            public int subtract(int a, int b) {
                                return a - b;
                            }
                        """
                );

        String updated =
                Files.readString(file);

        assertTrue(
                updated.contains(
                        "public int subtract"
                )
        );

        assertTrue(
                updated.contains(
                        "return a - b;"
                )
        );

        assertEquals(
                "Updated file: Calculator.java",
                result
        );
    }


    @Test
    void rejectsTextThatDoesNotExist()
            throws Exception {

        Path file =
                tempDirectory.resolve(
                        "Example.java"
                );

        Files.writeString(
                file,
                "public class Example {}"
        );

        Workspace workspace =
                new Workspace(tempDirectory);

        ReplaceTextTool tool =
                new ReplaceTextTool(workspace);

        assertThrows(
                IllegalArgumentException.class,
                () -> tool.replace(
                        "Example.java",
                        "something nonexistent",
                        "replacement"
                )
        );
    }


    @Test
    void rejectsAmbiguousReplacement()
            throws Exception {

        Path file =
                tempDirectory.resolve(
                        "Example.txt"
                );

        Files.writeString(
                file,
                """
                hello
                hello
                """
        );

        Workspace workspace =
                new Workspace(tempDirectory);

        ReplaceTextTool tool =
                new ReplaceTextTool(workspace);

        assertThrows(
                IllegalArgumentException.class,
                () -> tool.replace(
                        "Example.txt",
                        "hello",
                        "goodbye"
                )
        );
    }


    @Test
    void rejectsPathOutsideWorkspace()
            throws Exception {

        Workspace workspace =
                new Workspace(tempDirectory);

        ReplaceTextTool tool =
                new ReplaceTextTool(workspace);

        assertThrows(
                SecurityException.class,
                () -> tool.replace(
                        "../../../../etc/passwd",
                        "root",
                        "something"
                )
        );
    }
}
