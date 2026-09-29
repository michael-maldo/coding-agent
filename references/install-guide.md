Yes. Given your machine and your goal, I would build this **incrementally**. Don't start with RAG, pgvector, React, Tailscale, or even Spring Boot. First prove the most important loop:

> **Your local LLM can inspect a repository, modify code, run tests, see failures, and try again.**

Your current Fedora laptop—Ryzen 7 4800H, 38 GiB RAM, RTX 3050 4 GB—is enough for this first version. 

## Phase 1 — Get the local coding LLM working

We'll use **Ollama** initially because it's the easiest way to get local inference working.

### Step 1 — Install Ollama

On Fedora:

```bash
curl -fsSL https://ollama.com/install.sh | sh
```

Check it:

```bash
ollama --version
```

Then:

```bash
systemctl status ollama
```

If necessary:

```bash
sudo systemctl enable --now ollama
```

Don't install CUDA Toolkit yet. Your NVIDIA driver is already working, and Ollama doesn't require you to have `nvcc` just to use your GPU.

[Ollama Linux documentation](https://docs.ollama.com/linux?utm_source=chatgpt.com)

---

## Step 2 — Start with a small coding model

Because you only have **4 GB VRAM**, don't begin with a 30B model.

Start small so we establish that everything works.

For example:

```bash
ollama pull qwen2.5-coder:7b
```

Then:

```bash
ollama run qwen2.5-coder:7b
```

Ask it:

```text
Write a Java 21 method that takes a List<Integer>
and returns only the even numbers using streams.
```

We're not evaluating your agent yet. We're checking:

```text
Fedora
  ↓
Ollama
  ↓
model
  ↓
GPU/CPU
  ↓
response
```

While it's generating, open another terminal:

```bash
watch -n 1 nvidia-smi
```

That will show us whether Ollama is using your RTX 3050.

---

# Phase 2 — Talk to Ollama programmatically

Ollama exposes a local HTTP API.

Your application will eventually do something like:

```text
Your Agent
    │
    │ HTTP
    ▼
localhost:11434
    │
    ▼
Ollama
    │
    ▼
Coding model
```

Test it manually:

```bash
curl http://localhost:11434/api/chat \
  -d '{
    "model": "qwen2.5-coder:7b",
    "messages": [
      {
        "role": "user",
        "content": "Write a Java method that adds two integers."
      }
    ],
    "stream": false
  }'
```

If this works, you've already established the fundamental interface between your future agent and the LLM.

---

# Phase 3 — Create a dedicated agent project

I'd keep it separate from Nakorn Thai.

Something like:

```bash
mkdir -p ~/projects/coding-agent
cd ~/projects/coding-agent
git init
```

Eventually:

```text
coding-agent/
├── src/
├── prompts/
├── config/
├── docs/
└── pom.xml
```

Since you're comfortable with Spring, we can eventually make this a Spring Boot application.

But I recommend something even simpler first: a small Java CLI application.

```text
$ coding-agent ~/projects/nakorn-thai

Agent> What does this project do?

Agent> Find the menu endpoint.

Agent> Add validation to CreateMenuItemRequest.
```

No browser.

No React.

No authentication.

---

# Phase 4 — Give the LLM tools

This is the critical part.

The LLM shouldn't automatically get unrestricted shell access.

We create explicit tools.

Start with only:

```text
list_files
read_file
search_code
```

For example:

```java
interface AgentTool {

    String name();

    String execute(String arguments);
}
```

Then:

```java
class ReadFileTool implements AgentTool {

    private final Path workspace;

    @Override
    public String name() {
        return "read_file";
    }

    @Override
    public String execute(String path) throws IOException {

        Path requested =
            workspace.resolve(path).normalize();

        if (!requested.startsWith(workspace)) {
            throw new SecurityException(
                "Access outside workspace denied"
            );
        }

        return Files.readString(requested);
    }
}
```

That security check is important.

If your workspace is:

```text
~/projects/test-project
```

the model should **not** be able to request:

```text
../../.ssh/id_rsa
```

---

# Phase 5 — Let the model inspect a repository

Now give it a task:

> Find the implementation of GreetingService and explain how it works.

The interaction becomes:

```text
USER
 │
 │ "Find GreetingService"
 ▼
AGENT
 │
 ▼
LLM
 │
 │ requests:
 │ search_code("GreetingService")
 ▼
AGENT TOOL
 │
 ▼
repository
 │
 │ returns:
 │ src/main/java/.../GreetingService.java
 ▼
LLM
 │
 │ requests:
 │ read_file(...)
 ▼
AGENT
 │
 ▼
LLM
 │
 ▼
Answer
```

This is the first moment where you've actually built something **agent-like**.

---

# Phase 6 — Give it controlled editing

Only after reading/searching works properly, add:

```text
write_file
apply_patch
```

I strongly prefer `apply_patch`.

Instead of the model rewriting a 500-line Java file, it produces something conceptually like:

```diff
- return repository.findById(id);
+ return repository.findById(id)
+     .orElseThrow(() -> new UserNotFoundException(id));
```

Then your agent applies the patch.

Now:

```text
LLM
 ↓
read existing code
 ↓
propose patch
 ↓
agent applies patch
 ↓
git diff
```

---

# Phase 7 — Add Git

Add tools:

```text
git_status
git_diff
```

Initially, **do not allow**:

```text
git push
git reset --hard
git clean
git rebase
```

The agent can inspect changes but shouldn't be able to perform destructive operations.

Then every task can finish with:

```text
Modified files:

src/main/java/...
src/test/java/...

git diff:

...
```

---

# Phase 8 — Give it Maven

Now things become much more interesting.

Add a deliberately restricted command tool.

Don't initially give it:

```text
run_any_shell_command(String command)
```

Give it specific operations:

```text
maven_test
maven_compile
```

which internally execute:

```bash
./mvnw test
```

or:

```bash
mvn test
```

Now your agent can do:

```text
Task
 ↓
inspect code
 ↓
modify code
 ↓
mvn test
 ↓
FAIL
 ↓
read test output
 ↓
reason
 ↓
modify code
 ↓
mvn test
 ↓
PASS
```

**That is our first major milestone.**

---

# Phase 9 — Use a deliberately simple repository

Don't unleash it on Nakorn Thai immediately.

Use your `unit-testing-practice` project first. That's almost perfect for this because it's small and already has JUnit/Mockito material. 

Give your agent something like:

> Add a `subtract(int a, int b)` method to Calculator and create unit tests for it. Run all tests and don't finish until they pass.

We should be able to watch:

```text
Agent
 │
 ├─ list_files
 ├─ search_code Calculator
 ├─ read Calculator.java
 ├─ read CalculatorTest.java
 ├─ apply_patch
 ├─ apply_patch
 ├─ mvn_test
 │
 └─ PASS
```

If your own local agent accomplishes that autonomously, we've achieved something significant.

---

# Phase 10 — Add the agent loop

The central Java logic will eventually resemble:

```java
while (!finished) {

    LlmResponse response =
        llm.chat(conversation, availableTools);

    if (response.requestsTool()) {

        ToolCall call = response.toolCall();

        ToolResult result =
            toolRegistry.execute(call);

        conversation.add(result);

    } else {

        finished = true;
        System.out.println(response.text());
    }
}
```

Conceptually:

```text
             ┌───────────────┐
             │      LLM      │
             └───────┬───────┘
                     │
                what next?
                     │
             ┌───────▼───────┐
             │  Agent Loop   │
             └───────┬───────┘
                     │
       ┌─────────────┼──────────────┐
       ▼             ▼              ▼
   read_file     apply_patch     maven_test
       │             │              │
       └─────────────┼──────────────┘
                     │
                  result
                     │
                     └────────► LLM
```

That's essentially the engine of your coding agent.

---

# Phase 11 — THEN add RAG

Only after that works would I introduce the architecture we've spent time discussing.

```text
Spring Boot documentation
Java documentation
JUnit documentation
Mockito documentation
PostgreSQL documentation
        ↓
      ingest
        ↓
      chunks
        ↓
    embeddings
        ↓
PostgreSQL + pgvector
```

Then the agent gets another tool:

```text
search_documentation(query)
```

Example:

```text
LLM:

"I need to determine the recommended way to
configure Spring Security."

        ↓

search_documentation(
   "Spring Security SecurityFilterChain configuration"
)

        ↓

RAG

        ↓

Spring Security documentation excerpts

        ↓

LLM
```

Now you're combining:

```text
                    CODING AGENT

        ┌──────────────┴───────────────┐
        │                              │
     KNOWLEDGE                      ACTIONS
        │                              │
   RAG/search                      filesystem
   Spring docs                     Git
   Java docs                       Maven
   Mockito docs                    npm
   your standards                  tests
        │                              │
        └──────────────┬───────────────┘
                       │
                       ▼
                      LLM
```

---

# Phase 12 — Expand into full-stack development

Once Java works, add:

```text
npm_test
npm_build
playwright_test
```

and documentation for:

```text
React
Vite
JavaScript
Playwright
```

Then your agent can handle both:

```text
backend/
frontend/
```

and potentially perform tasks spanning the whole application.

---

## Don't build all of this now

For **tonight's first milestone**, I'd stop at:

```text
1. Install Ollama
        ↓
2. Pull 7B coding model
        ↓
3. Verify GPU/CPU usage
        ↓
4. Test Ollama HTTP API
```

Paste the output of:

```bash
ollama --version
```

and:

```bash
ollama run qwen2.5-coder:7b
```

along with `nvidia-smi` while it's generating.

We'll establish your **local inference layer first**. After that's working, the next piece I'd have you actually write is the tiny Java CLI agent with `read_file` and `search_code`—rather than jumping straight into the full architecture.
