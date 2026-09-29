package dev.gnostex.agent;

public interface AgentTool {

    String name();

    String execute(String argument) throws Exception;
}
