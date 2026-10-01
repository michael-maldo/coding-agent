package dev.gnostex.agent.workspace;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class AgentInstructionsTest {

    @TempDir
    Path tempDirectory;


    @Test
    void loadsRootAgentsFile()
            throws Exception {

        Files.writeString(
                tempDirectory.resolve(
                        "AGENTS.md"
                ),
                """
                # Agent Instructions

                Always inspect the Git diff.
                """
        );

        Workspace workspace =
                new Workspace(tempDirectory);

        AgentInstructions instructions =
                new AgentInstructions(workspace);

        String result =
                instructions.load();

        assertTrue(
                result.contains(
                        "Always inspect the Git diff."
                )
        );
    }


    @Test
    void returnsEmptyWhenAgentsFileDoesNotExist()
            throws Exception {

        Workspace workspace =
                new Workspace(tempDirectory);

        AgentInstructions instructions =
                new AgentInstructions(workspace);

        String result =
                instructions.load();

        assertEquals(
                "",
                result
        );
    }


    @Test
    void rejectsAgentsDirectory()
            throws Exception {

        Files.createDirectory(
                tempDirectory.resolve(
                        "AGENTS.md"
                )
        );

        Workspace workspace =
                new Workspace(tempDirectory);

        AgentInstructions instructions =
                new AgentInstructions(workspace);

        assertThrows(
                IllegalArgumentException.class,
                instructions::load
        );
    }
}
