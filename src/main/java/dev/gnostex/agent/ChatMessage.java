package dev.gnostex.agent;

public record ChatMessage(
        String role,
        String content
) {
}
