package dev.gnostex.agent.model;
import dev.gnostex.agent.core.AgentResponse;
import dev.gnostex.agent.core.ChatMessage;

import java.util.List;

public interface ModelProvider {

    AgentResponse chat(List<ChatMessage> messages)
            throws Exception;
}
