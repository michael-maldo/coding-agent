package dev.gnostex.agent;

import java.nio.file.Path;

public class Main {

    public static void main(String[] args)
            throws Exception {

        Workspace workspace =
                new Workspace(
                        Path.of(
                                System.getProperty("user.home"),
                                "Projects/current/agent-test"
                        )
                );

        AgentInstructions instructionLoader =
            new AgentInstructions(workspace);

        String repositoryInstructions =
            instructionLoader.load();

        ToolRegistry tools =
                new ToolRegistry();

        tools.register(
                new ReadFileTool(workspace)
        );

        tools.register(
                new SearchCodeTool(workspace)
        );

        tools.register(
                new ReplaceTextTool(workspace)
        );

        tools.register(
                new GitDiffTool(workspace)
        );

        OllamaClient ollama =
                new OllamaClient();

        CodingAgent agent =
                new CodingAgent(
                        ollama,
                        tools,
                        repositoryInstructions
                );

        String answer =
                agent.run(
                        """
                        Find the Calculator class.

                        Add a method:

                        public int subtract(int a, int b)

                        The method must return a - b.

                        Inspect the Git diff after making the change
                        before giving your final answer.
                        """
                );

        System.out.println(
                "\n========== FINAL ANSWER =========="
        );

        System.out.println(answer);
    }
}