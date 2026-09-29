Perfect. Your Java → Ollama integration is now clean:

```text
Java
  ↓
Jackson creates JSON
  ↓
HTTP → Ollama
  ↓
Qwen2.5-Coder
  ↓
Jackson parses JSON
  ↓
String answer
```

Now let's make the first **agent capability**: reading files from a repository.

## Step 7 — Create a Workspace

The `Workspace` represents the repository the agent is allowed to work on.

Create:

```text
src/main/java/dev/gnostex/agent/Workspace.java
```

```java
package dev.gnostex.agent;

import java.nio.file.Files;
import java.nio.file.Path;

public class Workspace {

    private final Path root;

    public Workspace(Path root) {

        this.root = root.toAbsolutePath().normalize();

        if (!Files.isDirectory(this.root)) {
            throw new IllegalArgumentException(
                    "Workspace does not exist: " + this.root
            );
        }
    }

    public Path resolve(String relativePath) {

        Path resolved =
                root.resolve(relativePath).normalize();

        if (!resolved.startsWith(root)) {
            throw new SecurityException(
                    "Access outside workspace denied: " + relativePath
            );
        }

        return resolved;
    }

    public Path root() {
        return root;
    }
}
```

That `startsWith(root)` check is important.

If the workspace is:

```text
/home/gnostex/unit-testing-practice
```

this is allowed:

```text
src/main/java/practice/Calculator.java
```

but this:

```text
../../.ssh/id_rsa
```

will resolve outside the workspace and be rejected.

---

## Step 8 — Create our tool interface

Create:

```text
src/main/java/dev/gnostex/agent/AgentTool.java
```

```java
package dev.gnostex.agent;

public interface AgentTool {

    String name();

    String execute(String argument) throws Exception;
}
```

We're intentionally keeping this primitive.

Eventually this interface will become more sophisticated, but right now we want to understand the mechanics.

---

## Step 9 — Implement `read_file`

Create:

```text
src/main/java/dev/gnostex/agent/ReadFileTool.java
```

```java
package dev.gnostex.agent;

import java.nio.file.Files;
import java.nio.file.Path;

public class ReadFileTool implements AgentTool {

    private final Workspace workspace;

    public ReadFileTool(Workspace workspace) {
        this.workspace = workspace;
    }

    @Override
    public String name() {
        return "read_file";
    }

    @Override
    public String execute(String argument) throws Exception {

        Path file = workspace.resolve(argument);

        if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException(
                    "File does not exist: " + argument
            );
        }

        return Files.readString(file);
    }
}
```

Notice the separation:

```text
ReadFileTool
     │
     │ "src/main/.../Calculator.java"
     ▼
Workspace
     │
     │ validates path
     ▼
filesystem
```

`ReadFileTool` doesn't decide which directories are safe.

`Workspace` owns that responsibility.

That's a useful architectural boundary.

---

# Step 10 — Test it WITHOUT the AI

This is important.

Before letting an LLM use a tool, **prove the tool works independently**.

Modify `Main.java` temporarily:

```java
package dev.gnostex.agent;

import java.nio.file.Path;

public class Main {

    public static void main(String[] args) throws Exception {

        Workspace workspace =
                new Workspace(
                        Path.of(System.getProperty("user.home"),
                                "unit-testing-practice")
                );

        ReadFileTool readFile =
                new ReadFileTool(workspace);

        String content =
                readFile.execute(
                        "src/main/java/practice/Calculator.java"
                );

        System.out.println(content);
    }
}
```

I'm using your earlier Termux project's structure as an example, so **verify the actual path on Fedora**. If your test project isn't at:

```text
~/unit-testing-practice
```

use whatever Fedora repository you want as the sandbox.

For example, if you want a completely fresh test repo:

```bash
mkdir -p ~/Projects/current/agent-test/src/main/java/practice
```

Create:

```text
~/Projects/current/agent-test/src/main/java/practice/Calculator.java
```

with:

```java
package practice;

public class Calculator {

    public int add(int a, int b) {
        return a + b;
    }
}
```

Then set:

```java
new Workspace(
    Path.of(System.getProperty("user.home"),
            "Projects/current/agent-test")
);
```

Run:

```bash
mvn compile
mvn exec:java -Dexec.mainClass=dev.gnostex.agent.Main
```

You should see:

```java
package practice;

public class Calculator {

    public int add(int a, int b) {
        return a + b;
    }
}
```

## Now deliberately attack it

Change:

```java
readFile.execute(
    "src/main/java/practice/Calculator.java"
);
```

to:

```java
readFile.execute(
    "../../../../etc/passwd"
);
```

Run again.

It **must fail** with something like:

```text
Access outside workspace denied
```

This is worth testing because later the request won't come from you:

```text
YOU
 ↓
LLM
 ↓
read_file(...)
 ↓
filesystem
```

An LLM is untrusted input as far as the tool layer is concerned.

So our fundamental rule is:

> **The agent decides what it wants to do; the tool layer decides what it is allowed to do.**

That distinction becomes extremely important once we add `write_file`, `run_command`, Maven, Git, etc.

---

## Don't connect this tool to Ollama yet

We're building this in layers:

```text
✓ Ollama
✓ Coding model
✓ Java → Ollama HTTP
✓ JSON handling

NOW
↓
Workspace
↓
read_file
↓
security boundary

NEXT
↓
LLM requests read_file
↓
Agent executes it
↓
result returned to LLM
```

Once your normal `Calculator.java` read succeeds **and** the `../../../../etc/passwd` test is rejected, show me both outputs.

Then we'll implement the really interesting part: **Qwen itself deciding that it needs to call `read_file`**.