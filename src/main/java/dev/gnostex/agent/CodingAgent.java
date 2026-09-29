package dev.gnostex.agent;

import java.util.ArrayList;
import java.util.List;

public class CodingAgent {

    private static final int MAX_STEPS = 10;

    private final OllamaClient ollama;
    private final ToolRegistry tools;

    public CodingAgent(
            OllamaClient ollama,
            ToolRegistry tools
    ) {
        this.ollama = ollama;
        this.tools = tools;
    }

    public String run(String userRequest)
            throws Exception {

        List<ChatMessage> messages =
                new ArrayList<>();

        /*
         * System instructions for the coding agent.
         *
         * These instructions teach the LLM how to use
         * the available tools and how to react to
         * tool results.
         */
        messages.add(
                new ChatMessage(
                        "system",
                        """
                        You are a coding agent working with a source-code repository.

                        You have tools that allow you to inspect and modify
                        the repository.

                        AVAILABLE TOOLS

                        search_code:
                        Searches the repository for exact text or symbols.

                        The search is literal. Overly descriptive search
                        queries may fail even when the requested code exists.


                        read_file:
                        Reads the contents of a file when its exact path
                        is known.


                        replace_text:
                        Replaces one exact unique block of text in an
                        existing file.

                        Use this tool when the user asks you to modify
                        existing source code.

                        The old_text argument must exactly match text that
                        currently exists in the file and must occur exactly
                        once.

                        Before using replace_text, normally read the target
                        file so that old_text is based on the actual current
                        file contents.


                        git_diff:
                        Shows the current Git diff.

                        Use this after modifying source code so that you can
                        inspect and verify the actual changes before claiming
                        that the task is complete.


                        RULES

                        1. Never conclude that code does not exist after
                           only one unsuccessful search.

                        2. When searching for a class, method, or symbol,
                           prefer the simplest identifying symbol.

                           Example:

                           User asks:
                           "Find the Calculator class."

                           Good search:
                           Calculator

                           Bad search:
                           Calculator class

                        3. If search_code returns:

                           No matches found.

                           reconsider the search query and try again using
                           a simpler or alternative search term.

                        4. If search_code returns a file path that appears
                           relevant to the user's request, use read_file
                           to inspect the actual file before explaining
                           or modifying its implementation.

                        5. Do not invent source code or file contents.

                        6. Base conclusions about the implementation on
                           actual repository content retrieved using the
                           available tools.

                        7. Use tools iteratively when necessary.

                           A typical read-only investigation might be:

                           search_code
                           -> search result
                           -> read_file
                           -> file contents
                           -> final answer

                        8. A failed tool call or unsuccessful search is
                           information, not necessarily the end of the task.
                           Reconsider your approach when appropriate.

                        9. When you have enough evidence to answer a
                           read-only request, answer normally instead of
                           requesting another tool.

                        10. When the user asks you to modify existing code,
                            do not merely show the user what the modified
                            code should look like.

                            You must use replace_text to actually modify
                            the repository.

                        11. Before using replace_text, read the target file
                            so that old_text is based on the actual current
                            contents of the file.

                        12. Make the smallest appropriate source-code
                            replacement needed to satisfy the request.

                        13. After every successful source-code modification,
                            use git_diff before claiming that the task is
                            complete.

                        14. Inspect the Git diff and verify that the actual
                            change matches the user's original request.

                        15. If replace_text fails, do not claim that the
                            modification succeeded.

                            Read the file again if necessary and reconsider
                            the exact old_text and new_text values.

                        16. A tool request must always contain both:

                            "name"
                            and
                            "arguments"

                            Even tools that require no arguments must use
                            an empty arguments object.

                            Correct example:

                            {"name":"git_diff","arguments":{}}

                        17. When the user asks you to change the repository,
                            do not give the user instructions telling them
                            to manually make the requested change.

                            Use the available tools to perform the change.

                        18. For a code modification task, a typical workflow
                            should be:

                            search_code
                            -> locate relevant file
                            -> read_file
                            -> understand current code
                            -> replace_text
                            -> git_diff
                            -> inspect the diff
                            -> final answer

                        19. Do not claim that a requested code modification
                            is complete unless replace_text confirmed the
                            modification and git_diff was inspected afterward.

                        Work iteratively until the user's original request
                        has been completed or the available repository
                        evidence genuinely prevents completion.
                        """
                )
        );

        /*
         * Add the user's original request to the
         * conversation.
         */
        messages.add(
                new ChatMessage(
                        "user",
                        userRequest
                )
        );


        /*
         * Agent loop.
         *
         * Each iteration gives the LLM another
         * opportunity to:
         *
         * - request a tool
         * - inspect a previous tool result
         * - modify code
         * - inspect modifications
         * - or provide the final answer
         */
        for (int step = 1;
             step <= MAX_STEPS;
             step++) {

            System.out.println(
                    "\n--- AGENT STEP " + step + " ---"
            );


            /*
             * Ask Qwen what it wants to do next.
             */
            AgentResponse response =
                    ollama.chat(messages);


            /*
             * If there is no tool call, Qwen believes
             * it has enough information to answer.
             */
            if (!response.hasToolCall()) {

                return response.content();
            }


            ToolCall call =
                    response.toolCall();


            System.out.println(
                    "Tool: " + call.name()
            );

            System.out.println(
                    "Arguments: " + call.arguments()
            );


            /*
             * Preserve Qwen's tool decision in the
             * conversation history.
             */
            messages.add(
                    new ChatMessage(
                            "assistant",
                            response.content()
                    )
            );


            /*
             * Execute the requested Java tool.
             */
            String toolResult;

            try {

                toolResult =
                        tools.execute(call);

            } catch (Exception e) {

                toolResult =
                        "Tool failed: "
                                + e.getMessage();
            }


            System.out.println(
                    "Result:\n" + toolResult
            );


            /*
             * Give the tool result back to Qwen.
             *
             * We currently represent tool results as
             * user messages because Qwen 2.5 Coder 7B
             * is returning tool requests as JSON inside
             * ordinary message content rather than
             * Ollama native tool_calls.
             */
            messages.add(
                    new ChatMessage(
                            "user",
                            """
                            TOOL RESULT

                            Tool:
                            %s

                            Result:
                            %s

                            Continue working on the ORIGINAL user request.

                            Important:

                            - Do not assume that one unsuccessful search
                              means the requested code does not exist.

                            - If a search returned no matches, consider
                              trying a simpler search term.

                            - If a search identified a relevant file,
                              inspect it with read_file when necessary.

                            - If the original request asks for a code change,
                              do not merely describe the change or show code
                              that the user should manually copy.

                            - For a code-change request, use replace_text to
                              actually modify the repository.

                            - Before replace_text, make sure you know the
                              actual current contents of the target file.

                            - If replace_text succeeds, inspect the actual
                              modification using git_diff before finishing.

                            - If replace_text fails, reconsider the exact
                              replacement and use additional tools if needed.

                            - A git_diff request must be:

                              {"name":"git_diff","arguments":{}}

                            - Do not claim a modification succeeded unless
                              the tool result confirms it.

                            - If the requested modification has been made
                              and the Git diff has been inspected, provide
                              the final answer.

                            - Otherwise request the next appropriate tool.
                            """.formatted(
                                    call.name(),
                                    toolResult
                            )
                    )
            );
        }


        /*
         * Prevent the model from entering an
         * unlimited tool-call loop.
         */
        throw new IllegalStateException(
                "Agent exceeded maximum steps: "
                        + MAX_STEPS
        );
    }
}