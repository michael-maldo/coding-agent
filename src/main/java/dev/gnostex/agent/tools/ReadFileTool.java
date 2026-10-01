package dev.gnostex.agent.tools;

import dev.gnostex.agent.workspace.Workspace;

import java.nio.file.Files;
import java.nio.file.Path;

public class ReadFileTool implements AgentTool {

    private final Workspace workspace;

    public ReadFileTool(Workspace workspace) {
        this.workspace = workspace;
    }

    @Override
    public String name() {
        return "read_file";
    }

    @Override
    public String execute(String argument) throws Exception {

        Path file = workspace.resolve(argument);

        if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException(
                    "File does not exist: " + argument
            );
        }

        return Files.readString(file);
    }
}
