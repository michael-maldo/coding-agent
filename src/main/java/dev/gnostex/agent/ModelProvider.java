package dev.gnostex.agent;

import java.util.List;

public interface ModelProvider {

    AgentResponse chat(List<ChatMessage> messages)
            throws Exception;
}
