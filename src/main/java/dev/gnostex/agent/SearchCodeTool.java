package dev.gnostex.agent;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class SearchCodeTool implements AgentTool {

    private final Workspace workspace;

    public SearchCodeTool(Workspace workspace) {
        this.workspace = workspace;
    }

    @Override
    public String name() {
        return "search_code";
    }

    @Override
    public String execute(String query) throws Exception {

        ProcessBuilder processBuilder =
                new ProcessBuilder(
                        "git",
                        "grep",
                        "-n",
                        "--",
                        query
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
                      .append(System.lineSeparator());
            }
        }

        int exitCode =
                process.waitFor();

        if (exitCode == 1) {
            return "No matches found.";
        }

        if (exitCode != 0) {
            throw new IllegalStateException(
                    "git grep failed with exit code "
                            + exitCode
                            + ":\n"
                            + output
            );
        }

        return output.toString();
    }
}
