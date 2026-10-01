package dev.gnostex.agent.core;

public record AgentResponse(
        String content,
        ToolCall toolCall
) {

    public boolean hasToolCall() {
        return toolCall != null;
    }
}
