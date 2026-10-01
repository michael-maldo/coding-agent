package dev.gnostex.agent.tools;

import dev.gnostex.agent.workspace.Workspace;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class GitDiffTool implements AgentTool {

    private final Workspace workspace;

    public GitDiffTool(Workspace workspace) {
        this.workspace = workspace;
    }

    @Override
    public String name() {
        return "git_diff";
    }

    @Override
    public String execute(String ignored)
            throws Exception {

        ProcessBuilder processBuilder =
                new ProcessBuilder(
                        "git",
                        "diff",
                        "--no-ext-diff",
                        "--"
                );

        processBuilder.directory(
                workspace.root().toFile()
        );

        processBuilder.redirectErrorStream(true);

        Process process =
                processBuilder.start();

        StringBuilder output =
                new StringBuilder();

        try (BufferedReader reader =
                     new BufferedReader(
                             new InputStreamReader(
                                     process.getInputStream(),
                                     StandardCharsets.UTF_8
                             )
                     )) {

            String line;

            while ((line = reader.readLine()) != null) {

                output.append(line)
                        .append(
                                System.lineSeparator()
                        );
            }
        }

        int exitCode =
                process.waitFor();

        if (exitCode != 0) {

            throw new IllegalStateException(
                    "git diff failed with exit code "
                            + exitCode
                            + ":\n"
                            + output
            );
        }

        if (output.isEmpty()) {
            return "No changes.";
        }

        return output.toString();
    }
}
