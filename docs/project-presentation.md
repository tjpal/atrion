# Atrion
Atrion is an educational project which implements a graph-based AI flow engine (similar in spirit to n8n or OpenAI's agent-builder)

![atrion-definition-mode](atrion-definition-mode.png)

## Used AI Tooling
* GitHub Copilot
* ChatGPT
* Adobe Firefly for icons
* **No** vibe coding was used

## Goals
Atrion is an educational project that I use for rapid prototyping of AI workflows. My primary goal is to gain a deeper understanding of failure classes, failure modes and corresponding mitigation strategies. I also intend to use Atrion long-term as the foundation for other projects (“eat your own dog food”).

By understanding the fundamental concepts and implementation details of such a tool, I aim to become familiar more quickly with similar existing solutions.

## Feature Overview
Atrion currently consists of a server component, a client for defining flows, and a UI client for human-in-the-loop operations.

**Server**
* Exposes a REST API for the Atrion Composer and Atrion UI client. Other applications can also use this API to upload or execute workflows.
* Executes all flow logic and serves as the central repository for graph nodes.
* Supported node types include:
    * Custom REST endpoint nodes for input/output
    * LLM execution
    * Jira nodes (trigger, read, write)
    * Human-in-the-loop feedback
    * Machine-in-the-loop node for automatic evaluation (testing, benchmarks)
* New node types can be added via a plugin system. Only the server needs to be modified, as the client pulls all required information from the server.
* Secret management for Jira credentials and similar information.
* Prompt repository management (right now based on folders which are usually git repositories).
* Tool support for LLM models
* LLM providers: Currently, only OpenAI’s API is supported. However, additional providers can be added easily. My next goal is to include Gemini and Claude.
* Status: A production-ready deployment is still in progress.

**Atrion Composer**
* Used to define graph-based flows and configure node parameters.
* The UI is implemented by using my own UI library ([composition](https://github.com/tjpal/composition)).

**Atrion UI**
* A user interface for executing Atrion flows and providing input data.
* Currently used primarily for human-in-the-loop review workflows.