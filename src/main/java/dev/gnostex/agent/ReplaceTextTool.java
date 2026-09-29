package dev.gnostex.agent;

import java.nio.file.Files;
import java.nio.file.Path;

public class ReplaceTextTool implements AgentTool {

    private final Workspace workspace;

    public ReplaceTextTool(Workspace workspace) {
        this.workspace = workspace;
    }

    @Override
    public String name() {
        return "replace_text";
    }

    /*
     * This method exists because AgentTool currently
     * requires execute(String).
     *
     * ReplaceTextTool actually needs three arguments,
     * so ToolRegistry will call replace(...) directly.
     */
    @Override
    public String execute(String ignored) {
        throw new UnsupportedOperationException(
                "replace_text requires path, old_text and new_text"
        );
    }

    public String replace(
            String relativePath,
            String oldText,
            String newText
    ) throws Exception {

        Path file =
                workspace.resolve(relativePath);

        if (!Files.exists(file)) {
            throw new IllegalArgumentException(
                    "File does not exist: "
                            + relativePath
            );
        }

        if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException(
                    "Not a regular file: "
                            + relativePath
            );
        }

        String content =
                Files.readString(file);

        int first =
                content.indexOf(oldText);

        if (first < 0) {
            throw new IllegalArgumentException(
                    "old_text was not found in file: "
                            + relativePath
            );
        }

        int second =
                content.indexOf(
                        oldText,
                        first + oldText.length()
                );

        if (second >= 0) {
            throw new IllegalArgumentException(
                    "old_text occurs more than once in file: "
                            + relativePath
            );
        }

        String updated =
                content.substring(0, first)
                        + newText
                        + content.substring(
                                first + oldText.length()
                        );

        Files.writeString(
                file,
                updated
        );

        return "Updated file: " + relativePath;
    }
}
