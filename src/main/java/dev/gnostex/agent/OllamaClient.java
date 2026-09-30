package dev.gnostex.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

public class OllamaClient implements ModelProvider {

    private static final URI GENERATE_URI =
            URI.create(
                    "http://localhost:11434/api/generate"
            );

    private static final URI CHAT_URI =
            URI.create(
                    "http://localhost:11434/api/chat"
            );

    private static final String MODEL =
            "qwen2.5-coder:7b";

    private final HttpClient httpClient =
            HttpClient.newHttpClient();

    private final ObjectMapper objectMapper =
            new ObjectMapper();


    /*
     * Simple text generation.
     *
     * This does not use tools.
     */
    public String generate(String prompt)
            throws IOException,
                   InterruptedException {

        String json =
                objectMapper.writeValueAsString(
                        Map.of(
                                "model", MODEL,
                                "prompt", prompt,
                                "stream", false
                        )
                );

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(GENERATE_URI)
                        .header(
                                "Content-Type",
                                "application/json"
                        )
                        .POST(
                                HttpRequest.BodyPublishers
                                        .ofString(json)
                        )
                        .build();

        HttpResponse<String> response =
                httpClient.send(
                        request,
                        HttpResponse.BodyHandlers
                                .ofString()

                );

if (response.statusCode() < 200
        || response.statusCode() >= 300) {

    throw new IOException(
            "Ollama request failed with HTTP "
                    + response.statusCode()
                    + ":\n"
                    + response.body()
    );
}

        JsonNode root =
                objectMapper.readTree(
                        response.body()
                );

        return root.path("response")
                .asText();
    }


    /*
     * Agent conversation.
     *
     * CodingAgent calls this repeatedly.
     *
     * Qwen may either:
     *
     * 1. request a tool, or
     * 2. return its final answer.
     */
    @Override
    public AgentResponse chat(
            List<ChatMessage> messages
    )
            throws IOException,
                   InterruptedException {


        /*
         * Tool #1
         *
         * Read a file when its exact path is known.
         */
        Map<String, Object> readFileTool =
                Map.of(
                        "type", "function",

                        "function", Map.of(

                                "name",
                                "read_file",

                                "description",
                                "Read the contents of a text file when the exact path is known.",

                                "parameters",
                                Map.of(
                                        "type",
                                        "object",

                                        "properties",
                                        Map.of(
                                                "path",
                                                Map.of(
                                                        "type",
                                                        "string",

                                                        "description",
                                                        "Path relative to the workspace root"
                                                )
                                        ),

                                        "required",
                                        List.of(
                                                "path"
                                        )
                                )
                        )
                );


        /*
         * Tool #2
         *
         * Search repository source code.
         */
        Map<String, Object> searchCodeTool =
                Map.of(
                        "type", "function",

                        "function", Map.of(

                                "name",
                                "search_code",

                                "description",
                                "Search project source code for an exact class name, method name, symbol, or text. Prefer short literal identifiers such as Calculator or subtract rather than descriptive phrases.",

                                "parameters",
                                Map.of(
                                        "type",
                                        "object",

                                        "properties",
                                        Map.of(
                                                "query",
                                                Map.of(
                                                        "type",
                                                        "string",

                                                        "description",
                                                        "Short literal text or symbol to search for, such as Calculator or subtract"
                                                )
                                        ),

                                        "required",
                                        List.of(
                                                "query"
                                        )
                                )
                        )
                );


        /*
         * Tool #3
         *
         * Controlled source-code modification.
         *
         * The old text must match exactly once.
         */
        Map<String, Object> replaceTextTool =
                Map.of(
                        "type", "function",

                        "function", Map.of(

                                "name",
                                "replace_text",

                                "description",
                                "Replace one exact unique block of text in an existing file.",

                                "parameters",
                                Map.of(
                                        "type",
                                        "object",

                                        "properties",
                                        Map.of(
                                                "path",
                                                Map.of(
                                                        "type",
                                                        "string",

                                                        "description",
                                                        "Path relative to the workspace root"
                                                ),

                                                "old_text",
                                                Map.of(
                                                        "type",
                                                        "string",

                                                        "description",
                                                        "Exact existing text to replace. It must occur exactly once."
                                                ),

                                                "new_text",
                                                Map.of(
                                                        "type",
                                                        "string",

                                                        "description",
                                                        "Replacement text"
                                                )
                                        ),

                                        "required",
                                        List.of(
                                                "path",
                                                "old_text",
                                                "new_text"
                                        )
                                )
                        )
                );


        /*
         * Tool #4
         *
         * Inspect modifications made to the repository.
         */
        Map<String, Object> gitDiffTool =
                Map.of(
                        "type", "function",

                        "function", Map.of(

                                "name",
                                "git_diff",

                                "description",
                                "Show the current Git diff so you can inspect source-code changes.",

                                "parameters",
                                Map.of(
                                        "type",
                                        "object",

                                        "properties",
                                        Map.of()
                                )
                        )
                );


        /*
         * Convert our ChatMessage objects into
         * the format expected by Ollama.
         */
        List<Map<String, String>> messageMaps =
                messages.stream()
                        .map(
                                message ->
                                        Map.of(
                                                "role",
                                                message.role(),

                                                "content",
                                                message.content()
                                        )
                        )
                        .toList();


        /*
         * Build the Ollama chat request.
         *
         * These are the tools currently exposed
         * to the model.
         */
        Map<String, Object> requestBody =
                Map.of(
                        "model",
                        MODEL,

                        "messages",
                        messageMaps,

                        "tools",
                        List.of(
                                readFileTool,
                                searchCodeTool,
                                replaceTextTool,
                                gitDiffTool
                        ),

                        "stream",
                        false
                );


        String json =
                objectMapper.writeValueAsString(
                        requestBody
                );


        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(CHAT_URI)
                        .header(
                                "Content-Type",
                                "application/json"
                        )
                        .POST(
                                HttpRequest.BodyPublishers
                                        .ofString(json)
                        )
                        .build();


        HttpResponse<String> response =
                httpClient.send(
                        request,
                        HttpResponse.BodyHandlers
                                .ofString()
                );


        /*
         * Reject HTTP failures before attempting
         * to interpret the response as an agent
         * message.
         */
        if (response.statusCode() < 200
                || response.statusCode() >= 300) {

            throw new IOException(
                    "Ollama request failed with HTTP "
                            + response.statusCode()
                            + ":\n"
                            + response.body()
            );
        }


        JsonNode root =
                objectMapper.readTree(
                        response.body()
                );


        JsonNode message =
                root.path(
                        "message"
                );


        String content =
                message.path(
                        "content"
                )
                .asText();


        /*
         * Preferred tool-call mechanism:
         *
         * Ollama native structured tool calling.
         *
         * A response may contain:
         *
         * message.tool_calls[]
         *
         * with:
         *
         * function.name
         * function.arguments
         */
        JsonNode toolCalls =
                message.path(
                        "tool_calls"
                );


        if (toolCalls.isArray()
                && !toolCalls.isEmpty()) {

            /*
             * Our current CodingAgent executes one
             * tool at a time.
             *
             * Therefore process the first requested
             * tool call.
             */
            JsonNode firstToolCall =
                    toolCalls.get(0);


            JsonNode function =
                    firstToolCall.path(
                            "function"
                    );


            String toolName =
                    function.path(
                            "name"
                    )
                    .asText();


            JsonNode arguments =
                    function.path(
                            "arguments"
                    );


            if (!toolName.isBlank()) {

                /*
                 * Normally arguments should be a JSON
                 * object.
                 *
                 * Some model/runtime combinations may
                 * return JSON encoded inside a string,
                 * so support that representation too.
                 */
                if (arguments.isTextual()) {

                    String argumentText =
                            arguments.asText();

                    if (argumentText.isBlank()) {

                        arguments =
                                objectMapper
                                        .createObjectNode();

                    } else {

                        arguments =
                                objectMapper.readTree(
                                        argumentText
                                );
                    }
                }


                /*
                 * A no-argument tool such as git_diff
                 * should receive an empty object.
                 */
                if (arguments.isMissingNode()
                        || arguments.isNull()) {

                    arguments =
                            objectMapper
                                    .createObjectNode();
                }


                if (!arguments.isObject()) {

                    throw new IOException(
                            "Invalid arguments for tool "
                                    + toolName
                                    + ": "
                                    + arguments
                    );
                }


                ToolCall toolCall =
                        new ToolCall(
                                toolName,
                                arguments
                        );


                return new AgentResponse(
                        content,
                        toolCall
                );
            }
        }


        /*
         * Compatibility fallback.
         *
         * During the earlier version of the coding
         * agent, Qwen returned tool requests as JSON
         * inside ordinary message.content.
         *
         * Example:
         *
         * {
         *   "name": "read_file",
         *   "arguments": {
         *     "path": "src/main/java/..."
         *   }
         * }
         *
         * Keep supporting this representation for
         * now while native tool calling is tested.
         */
        if (!content.isBlank()) {

            try {

                JsonNode toolJson =
                        objectMapper.readTree(
                                content
                        );


                if (toolJson.isObject()
                        && toolJson.has("name")
                        && toolJson.has(
                                "arguments"
                        )) {

                    String toolName =
                            toolJson.path(
                                    "name"
                            )
                            .asText();


                    JsonNode arguments =
                            toolJson.path(
                                    "arguments"
                            );


                    if (!toolName.isBlank()
                            && arguments.isObject()) {

                        ToolCall toolCall =
                                new ToolCall(
                                        toolName,
                                        arguments
                                );


                        return new AgentResponse(
                                content,
                                toolCall
                        );
                    }
                }

            } catch (Exception ignored) {

                /*
                 * message.content is ordinary text,
                 * not our legacy JSON tool-call
                 * representation.
                 */
            }
        }


        /*
         * No native structured tool call and no
         * legacy JSON tool call.
         *
         * Therefore treat message.content as the
         * model's final response.
         */
        return new AgentResponse(
                content,
                null
        );
    }
}