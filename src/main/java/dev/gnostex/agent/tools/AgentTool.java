package dev.gnostex.agent.tools;

public interface AgentTool {

    String name();

    String execute(String argument) throws Exception;
}
