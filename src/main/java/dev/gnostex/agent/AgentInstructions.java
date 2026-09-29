package dev.gnostex.agent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class AgentInstructions {

    private static final String FILE_NAME =
            "AGENTS.md";

    private final Workspace workspace;

    public AgentInstructions(
            Workspace workspace
    ) {
        this.workspace = workspace;
    }

    public String load()
            throws IOException {

        Path file =
                workspace.root()
                        .resolve(FILE_NAME)
                        .normalize();

        if (!file.startsWith(
                workspace.root()
        )) {
            throw new SecurityException(
                    "Instruction file outside workspace"
            );
        }

        if (!Files.exists(file)) {
            return "";
        }

        if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException(
                    "AGENTS.md is not a regular file"
            );
        }

        return Files.readString(file);
    }
}
