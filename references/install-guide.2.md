Excellent. You've now proven the complete local programmatic path:

```text
Java program
    ↓
HTTP
    ↓
Ollama :11434
    ↓
Qwen2.5-Coder 7B
    ↓
RTX 3050 / CPU
    ↓
JSON response
    ↓
Java program
```

And the response metadata gives us another useful measurement:

```text
Generation: 82 tokens
Generation time: ~7.53 sec
≈ 10.9 tokens/sec
```

Very consistent with your earlier **10.5 tokens/sec** benchmark.

Now let's improve the code before adding tools.

## Step 6 — Parse JSON properly

Right now `OllamaClient` returns this entire thing:

```json
{
  "model": "...",
  "response": "Dependency injection...",
  "done": true,
  ...
}
```

We want:

```java
String answer = ollama.generate(prompt);
```

to return only:

```text
Dependency injection in Spring Boot...
```

Let's use Jackson rather than manually parsing JSON.

Add this dependency to your `pom.xml`:

```xml
<dependencies>
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
        <version>2.20.0</version>
    </dependency>
</dependencies>
```

Now replace `OllamaClient.java` with:

```java
package dev.gnostex.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public class OllamaClient {

    private static final URI OLLAMA =
            URI.create("http://localhost:11434/api/generate");

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String generate(String prompt)
            throws IOException, InterruptedException {

        String json = objectMapper.writeValueAsString(
                Map.of(
                        "model", "qwen2.5-coder:7b",
                        "prompt", prompt,
                        "stream", false
                )
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(OLLAMA)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response =
                httpClient.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

        JsonNode root =
                objectMapper.readTree(response.body());

        return root.get("response").asText();
    }
}
```

Notice we've improved **two things**, not one.

Previously we manually escaped:

```java
String escapedPrompt = prompt
    .replace(...)
```

That's fragile.

Now Jackson creates the request JSON:

```java
objectMapper.writeValueAsString(...)
```

and parses the response JSON:

```java
objectMapper.readTree(...)
```

Much better.

## Compile and run

Because we now have a dependency, don't run with only:

```bash
java -cp target/classes ...
```

yet—that classpath won't include Jackson.

The simplest approach is to add the Maven exec plugin:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.codehaus.mojo</groupId>
            <artifactId>exec-maven-plugin</artifactId>
            <version>3.5.0</version>
        </plugin>
    </plugins>
</build>
```

Then:

```bash
mvn compile
```

and:

```bash
mvn exec:java -Dexec.mainClass=dev.gnostex.agent.Main
```

Now the output should simply be:

```text
Asking local AI...

Dependency injection in Spring Boot is...
```

rather than the entire Ollama JSON structure.

---

# Step 7 will be much more interesting

Once this works, we're going to stop asking the LLM generic questions.

We'll give it a **workspace**.

For example:

```text
~/unit-testing-practice
```

Then we'll create:

```java
Workspace
```

which establishes the boundary:

```text
coding-agent
      │
      ▼
Workspace
      │
      ▼
~/unit-testing-practice
      │
      ├── pom.xml
      └── src/
```

And then our first real agent tool:

```java
ReadFileTool
```

so the LLM can effectively request:

```text
read_file("src/main/java/practice/Calculator.java")
```

and receive the actual contents.

That's the point where our architecture changes from:

```text
User → LLM → text
```

to:

```text
User
 ↓
LLM
 ↓
"I need Calculator.java"
 ↓
AGENT
 ↓
filesystem
 ↓
Calculator.java
 ↓
LLM
 ↓
reason about actual project
```

That will be the **first genuine agent capability**.

Get the Jackson version compiling and show me the output from:

```bash
mvn exec:java -Dexec.mainClass=dev.gnostex.agent.Main
```

Then we'll implement the workspace boundary and `read_file`.