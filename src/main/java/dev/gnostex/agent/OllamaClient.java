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

public class OllamaClient {

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
    public AgentResponse chat(
            List<ChatMessage> messages
    )
            throws IOException,
                   InterruptedException {


        /*
         * Tool #1
         *
         * Read a file when the path is already known.
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
                                "Search project source code for a class, method, symbol, or text.",

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
                                                        "Text or symbol to search for"
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
         * Build Ollama request.
         *
         * IMPORTANT:
         *
         * This is the ONE tool list used by the
         * current CodingAgent architecture.
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


        JsonNode root =
                objectMapper.readTree(
                        response.body()
                );


        String content =
                root.path("message")
                        .path("content")
                        .asText();


        /*
         * Qwen 2.5 Coder 7B is currently returning
         * its requested tool as JSON inside the
         * ordinary message content.
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
         * Try to interpret the response as a
         * ToolCall.
         *
         * If it isn't one, treat the response as
         * the model's final answer.
         */
        try {

            JsonNode toolJson =
                    objectMapper.readTree(
                            content
                    );

            if (toolJson.has("name")
                    && toolJson.has("arguments")) {

                ToolCall toolCall =
                        new ToolCall(
                                toolJson.path("name")
                                        .asText(),

                                toolJson.path(
                                        "arguments"
                                )
                        );

                return new AgentResponse(
                        content,
                        toolCall
                );
            }

        } catch (Exception ignored) {

            /*
             * Normal text response.
             *
             * It is not JSON representing a tool call.
             */
        }


        /*
         * No tool request.
         *
         * Therefore this is the agent's final answer.
         */
        return new AgentResponse(
                content,
                null
        );
    }
}