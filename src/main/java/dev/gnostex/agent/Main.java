package dev.gnostex.agent;

import java.nio.file.Path;
import java.util.Scanner;

public class Main {

    public static void main(String[] args)
            throws Exception {

        /*
         * Repository the agent is allowed to access.
         */
        Workspace workspace =
                new Workspace(
                        Path.of(
                                System.getProperty("user.home"),
                                "Projects/current/agent-test"
                        )
                );


        /*
         * Load repository-specific instructions
         * from AGENTS.md.
         */
        AgentInstructions instructionLoader =
            new AgentInstructions(workspace);

        String repositoryInstructions =
            instructionLoader.load();


        /*
         * Register the tools that Qwen is allowed
         * to use.
         */
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


        /*
         * Local Ollama client.
         */
        OllamaClient ollama =
                new OllamaClient();


        /*
         * Create the coding agent.
         */
        CodingAgent agent =
                new CodingAgent(
                        ollama,
                        tools,
                        repositoryInstructions
                );


        /*
         * Interactive command-line interface.
         *
         * Each user message becomes a new agent task.
         */
        try (Scanner scanner =
                     new Scanner(System.in)) {

            System.out.println(
                    """
                    
                    ========================================
                    Gnostex Coding Agent
                    ========================================

                    Workspace:
                    %s

                    Model:
                    qwen2.5-coder:7b

                    Commands:
                      exit   Exit the agent
                      quit   Exit the agent

                    Enter a coding request below.
                    """.formatted(
                            workspace.root()
                    )
            );


            /*
             * Outer conversation loop.
             *
             * CodingAgent.run() contains the inner
             * tool/reasoning loop for each individual
             * request.
             */
            while (true) {

                System.out.print(
                        "\nYou> "
                );


                /*
                 * Handles Ctrl+D / EOF cleanly.
                 */
                if (!scanner.hasNextLine()) {

                    System.out.println(
                            "\nExiting coding agent."
                    );

                    break;
                }


                String userRequest =
                        scanner.nextLine()
                                .trim();


                /*
                 * Ignore empty input.
                 */
                if (userRequest.isBlank()) {
                    continue;
                }


                /*
                 * Exit commands.
                 */
                if (userRequest.equalsIgnoreCase(
                        "exit"
                )
                        || userRequest.equalsIgnoreCase(
                        "quit"
                )) {

                    System.out.println(
                            "Exiting coding agent."
                    );

                    break;
                }


                /*
                 * Execute one complete coding-agent
                 * task.
                 */
                try {

                    String answer =
                            agent.run(
                                    userRequest
                );

        System.out.println(
                "\nAgent>"
        );

                    System.out.println(
                            answer
                    );

                } catch (Exception e) {

                    /*
                     * A failed task should not terminate
                     * the entire interactive application.
                     *
                     * The user can enter another request.
                     */
                    System.err.println(
                            "\nAgent task failed: "
                                    + e.getMessage()
                    );
                }
            }
        }
    }
}