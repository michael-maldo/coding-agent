package dev.gnostex.agent.core;

import com.fasterxml.jackson.databind.JsonNode;

public record ToolCall(
        String name,
        JsonNode arguments
) {

    public String argument(String name) {

        JsonNode value = arguments.get(name);

        if (value == null) {
            throw new IllegalArgumentException(
                    "Missing tool argument: " + name
            );
        }

        return value.asText();
    }
}