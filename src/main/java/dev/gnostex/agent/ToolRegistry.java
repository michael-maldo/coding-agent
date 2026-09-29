package dev.gnostex.agent;

import java.util.HashMap;
import java.util.Map;

public class ToolRegistry {

    private final Map<String, AgentTool> tools =
            new HashMap<>();

    public void register(AgentTool tool) {
        tools.put(tool.name(), tool);
    }

    public String execute(ToolCall call)
            throws Exception {

        AgentTool tool =
                tools.get(call.name());

        if (tool == null) {
            throw new IllegalArgumentException(
                    "Unknown tool: " + call.name()
            );
        }

        return switch (call.name()) {

            case "read_file" ->
                    tool.execute(
                            call.argument("path")
                    );

            case "search_code" ->
                    tool.execute(
                            call.argument("query")
                    );

            case "replace_text" -> {

                if (!(tool instanceof ReplaceTextTool replaceTextTool)) {
                    throw new IllegalStateException(
                            "replace_text registered with wrong implementation"
                    );
                }

                yield replaceTextTool.replace(
                        call.argument("path"),
                        call.argument("old_text"),
                        call.argument("new_text")
                );
            }

            case "git_diff" ->
                tool.execute("");

            default ->
                    throw new IllegalArgumentException(
                            "Unsupported tool: "
                                    + call.name()
                    );
        };
    }
}
