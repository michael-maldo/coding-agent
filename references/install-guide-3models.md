Yes. Let's do it in controlled stages and **not change your Java coding-agent yet**. First install and verify the two stronger local models. Then we'll benchmark them. Only after that will we implement `ModelProvider` + routing.

Your target setup is:

```text
Ollama
├── qwen2.5-coder:7b     ← routine baseline
├── devstral:24b         ← strong candidate A
└── qwen3-coder:30b      ← strong candidate B
```

## Phase 1 — Check available resources

Before downloading anything:

```bash
free -h
df -h ~
nvidia-smi
ollama list
```

You already have roughly 40 GB RAM and RTX 3050 4 GB VRAM. Pay particular attention to free disk space because the two additional models consume tens of GB on disk.

Your existing model should show:

```text
qwen2.5-coder:7b
```

## Phase 2 — Install Devstral 24B

Run:

```bash
ollama pull devstral:24b
```

This will take a while because the model is much larger than your 7B model.

When complete:

```bash
ollama list
```

You should now have:

```text
qwen2.5-coder:7b
devstral:24b
```

### Test Devstral

Run:

```bash
ollama run devstral:24b
```

Give it:

```text
Write a Java 21 Spring Boot service called UserService.

Requirements:
- use constructor injection
- depend on UserRepository
- implement findById(Long id)
- throw UserNotFoundException if missing
- implement createUser(String name, String email)
- do not use field injection
- return only Java code
```

Then:

```text
/bye
```

Immediately check:

```bash
ollama ps
```

and:

```bash
nvidia-smi
```

Save that output. It will tell us how much is GPU vs CPU/offload.

Then unload it:

```bash
ollama stop devstral:24b
```

Confirm:

```bash
ollama ps
```

---

# Phase 3 — Install Qwen3-Coder 30B

Now:

```bash
ollama pull qwen3-coder:30b
```

When finished:

```bash
ollama list
```

We want all three:

```text
qwen2.5-coder:7b
devstral:24b
qwen3-coder:30b
```

### Test Qwen3-Coder

Run:

```bash
ollama run qwen3-coder:30b
```

Use **exactly the same prompt**:

```text
Write a Java 21 Spring Boot service called UserService.

Requirements:
- use constructor injection
- depend on UserRepository
- implement findById(Long id)
- throw UserNotFoundException if missing
- implement createUser(String name, String email)
- do not use field injection
- return only Java code
```

Then:

```text
/bye
```

Again:

```bash
ollama ps
nvidia-smi
```

Then:

```bash
ollama stop qwen3-coder:30b
```

---

# Phase 4 — Verify the final Ollama environment

Run:

```bash
ollama list
```

Your environment should conceptually be:

```text
Ollama :11434
   │
   ├── qwen2.5-coder:7b
   │
   ├── devstral:24b
   │
   └── qwen3-coder:30b
```

Nothing in your Java application has changed yet.

That's intentional.

---

# Phase 5 — Compare raw reasoning first

Before letting the models modify repositories, give all three the same reasoning problem.

Use this prompt:

```text
You are reviewing a Java 21 Spring Boot application.

UserService checks:

    if (userRepository.existsByEmail(email)) {
        throw new DuplicateEmailException(email);
    }

and then calls:

    userRepository.save(user);

The email column currently has no UNIQUE database constraint.

Explain whether this correctly guarantees that duplicate email
addresses cannot be created when two requests execute concurrently.

If it is unsafe, explain the failure scenario and recommend a robust
Spring Boot/PostgreSQL solution.

Be precise.
```

Run:

```bash
ollama run qwen2.5-coder:7b
```

Paste the prompt.

Then:

```text
/bye
```

Next:

```bash
ollama stop qwen2.5-coder:7b
ollama run devstral:24b
```

Give it the **identical prompt**.

Then:

```text
/bye
```

Finally:

```bash
ollama stop devstral:24b
ollama run qwen3-coder:30b
```

Same prompt again.

Don't change the wording between models. We're starting to establish a controlled benchmark.

---

# Phase 6 — Don't choose the winner yet

One prompt tells us almost nothing.

Eventually we want something like:

| Test | 7B | Devstral 24B | Qwen3 30B |
|---|---|---|---|
| Simple Java | | | |
| Spring service | | | |
| JUnit | | | |
| Mockito | | | |
| Transaction reasoning | | | |
| Security | | | |
| Multi-file change | | | |
| Failed-test repair | | | |
| Architecture | | | |
| React | | | |

We're interested primarily in:

**correctness > architecture > test quality > minimal changes > successful repair**

You specifically said speed isn't important, so don't reject the larger model because it takes several minutes.

---

# Phase 7 — Then refactor your Java architecture

Once both models run successfully, **then** we'll change your coding-agent.

Currently:

```text
CodingAgent
     ↓
OllamaClient
     ↓
qwen2.5-coder:7b
```

We'll introduce:

```text
CodingAgent
     ↓
ModelProvider
     ↓
ModelRouter
     │
     ├── ROUTINE
     │      ↓
     │  qwen2.5-coder:7b
     │
     └── STRONG
            ↓
        selected larger model
```

The first interface will look roughly like:

```java
public interface ModelProvider {

    AgentResponse chat(
            List<ChatMessage> messages
    ) throws Exception;
}
```

Then our Ollama implementation will carry the model name:

```text
OllamaModelProvider
        │
        ├── qwen2.5-coder:7b
        ├── devstral:24b
        └── qwen3-coder:30b
```

We will **not duplicate your agent**.

There will still only be one:

```text
CodingAgent
```

and one:

```text
ToolRegistry
```

and one set of:

```text
search_code
read_file
replace_text
git_diff
maven_test
...
```

The model becomes interchangeable.

---

# Phase 8 — Router V1

Initially we'll keep routing intentionally simple.

Something like:

```text
User task
    ↓
TaskRouter
    ↓
normal?
 ┌──┴──┐
yes    no
 ↓      ↓
7B    strong
```

But the router won't just search for words such as `"hard"`.

We'll eventually have an execution state such as:

```text
steps
searches
filesRead
filesChanged
toolFailures
compileFailures
testFailures
repairAttempts
```

That lets us do:

```text
Start with 7B
     ↓
search/read/edit
     ↓
maven_test
     ↓
PASS ──────────────→ finish
     ↓
FAIL
     ↓
7B repairs
     ↓
maven_test
     ↓
PASS ──────────────→ finish
     ↓
FAIL again
     ↓
ESCALATE
     ↓
strong local model
```

That's much better than guessing task difficulty from the user's sentence.

---

# Phase 9 — Eventually Codex becomes Tier 3

Once local routing works:

```text
                       CodingAgent
                            │
                            ▼
                          Router
                            │
                  ┌─────────┼─────────┐
                  ▼         ▼         ▼
               ROUTINE    STRONG    EXPERT
                  │         │         │
                  ▼         ▼         ▼
              Qwen 7B    24/30B     Codex
                 local     local
```

Then your platform can behave like:

```text
Try inexpensive/local intelligence
              ↓
verify objectively
              ↓
if struggling
              ↓
strong local intelligence
              ↓
verify objectively
              ↓
if still struggling
              ↓
Codex
```

The key principle is:

> **The router should escalate based primarily on evidence that the current model cannot reliably complete the task—not merely on how complicated the original prompt sounds.**

For now, stop after **Phase 3**. Install both models, run the identical `UserService` test on each, and show me:

```bash
ollama list
ollama ps
```

after testing the 30B model. From that we'll confirm that your laptop can actually operate both candidates reliably before touching the coding-agent architecture.