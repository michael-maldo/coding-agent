package dev.gnostex.agent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Workspace {

    private final Path root;
    private final Path realRoot;

    public Workspace(Path root) {

        this.root =
                root.toAbsolutePath()
                        .normalize();

        if (!Files.isDirectory(this.root)) {
            throw new IllegalArgumentException(
                    "Workspace does not exist: "
                            + this.root
            );
        }

        try {

            this.realRoot =
                    this.root.toRealPath();

        } catch (IOException e) {

            throw new IllegalArgumentException(
                    "Unable to resolve workspace: "
                            + this.root,
                    e
            );
        }
    }


    public Path resolve(String relativePath) {

        Path resolved =
                root.resolve(relativePath)
                        .normalize();

        /*
         * First boundary:
         *
         * Prevent ordinary ../ path traversal.
         */
        if (!resolved.startsWith(root)) {

            throw new SecurityException(
                    "Access outside workspace denied: "
                            + relativePath
            );
        }

        /*
         * If the target already exists, resolve
         * symbolic links and verify that the real
         * target remains inside the workspace.
         */
        if (Files.exists(resolved)) {

            try {

                Path realResolved =
                        resolved.toRealPath();

                if (!realResolved.startsWith(realRoot)) {

                    throw new SecurityException(
                            "Symlink access outside workspace denied: "
                                    + relativePath
                    );
                }

                return realResolved;

            } catch (IOException e) {

                throw new IllegalArgumentException(
                        "Unable to resolve path: "
                                + relativePath,
                        e
                );
            }
        }

        /*
         * For a path that does not exist yet, return
         * the normalized path.
         *
         * This will become important later when we
         * allow the agent to create new files.
         */
        return resolved;
    }


    public Path root() {
        return root;
    }
}