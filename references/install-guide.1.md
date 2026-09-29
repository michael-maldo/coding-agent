Everything is working correctly.

The important lines are:

```text
Active: active (running)
Listening on 127.0.0.1:11434
discovering available GPUs...
inference compute id=0
vram-based default context ... num_ctx=4096
```

And:

```json
{"models":[]}
```

means exactly what we expected: Ollama is running, but no model has been downloaded.

Also note that Ollama sees about **3.5 GiB usable VRAM** from your nominal 4 GB GPU because some VRAM is being used by your Fedora/KDE graphical desktop. That's normal.

## Step 3 — Download our first coding model

Let's start with the 7B model rather than something tiny:

```bash
ollama pull qwen2.5-coder:7b
```

This will take a while depending on your connection.

When finished, check:

```bash
ollama list
```

You should see `qwen2.5-coder:7b`.

Then start it:

```bash
ollama run qwen2.5-coder:7b
```

When you get the:

```text
>>>
```

prompt, give it this very simple test:

```text
Write a Java 21 class called Calculator with an add(int a, int b)
method. Return only the Java code.
```

### While it's generating

Open a **second terminal** and run:

```bash
watch -n 1 nvidia-smi
```

And in a third terminal, run:

```bash
ollama ps
```

`ollama ps` is especially useful because it can tell us whether the model is running on the GPU, CPU, or a mixture.

We're looking for something along the lines of:

```text
NAME                 PROCESSOR
qwen2.5-coder:7b     40%/60% CPU/GPU
```

Don't worry if much of the model is in CPU/system RAM. With only ~3.5 GiB usable VRAM, I expect the 7B model to require some offloading.

Run those and send me:

```text
ollama list
ollama ps
```

plus what `nvidia-smi` shows while the model is actually generating.

Then we'll know **realistically how well your laptop handles a 7B coding model**, rather than estimating it. After that, we'll make our first API call and start building the agent.