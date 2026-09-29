package dev.gnostex.agent;

public record AgentResponse(
        String content,
        ToolCall toolCall
) {

    public boolean hasToolCall() {
        return toolCall != null;
    }
}
