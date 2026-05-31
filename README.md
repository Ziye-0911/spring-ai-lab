# Spring AI Lab

> Spring AI Lab — AI scenario starters for Spring Boot. Chat, RAG, multi-agent, code review, and more — each available as a drop-in dependency with auto-configured REST APIs. Zero boilerplate, production-ready.
>
> **Spring AI Application Rapid Development Toolkit**

---

## Table of Contents

- [Overview](#overview)
- [Core Features](#core-features)
- [Quick Start](#quick-start)
- [Integration Guide](#integration-guide)
- [Module Structure](#module-structure)
- [Architecture](#architecture)
- [API Reference](#api-reference)
- [Configuration Reference](#configuration-reference)
- [Observability](#observability)
- [Tech Stack](#tech-stack)
- [Contributing](#contributing)
- [Author](#author)

---

## Overview

Spring AI Lab is a rapid development toolkit built on top of Spring AI. It encapsulates recurring AI application patterns into plug-and-play **scenario starters**, so developers can focus on business logic — not infrastructure.

### Design Principles

- **Convention over Configuration** — every scenario starter works out of the box; just provide an API key.
- **Progressive** — use a single starter, or mix and match multiple.
- **Stand on the shoulders of giants** — 100% based on the official Spring AI API; we only compose and wrap.
- **Independently runnable** — each scenario starter is a self-contained module with no cross-scenario dependencies.
- **Core defines only abstractions** — the core module ships zero heavyweight third-party implementations.

### Relationship with Spring AI

```
Spring AI (Official)
├── ChatClient  ────┐
├── Tool Calling ───┤
├── RAG / Vector ───┤── Spring AI Lab (this project)
├── MCP ────────────┤    │
└── Advisors ───────┘    ├── Scenario starters (compose official APIs into ready-to-use products)
                          ├── Unified configuration (simplify multi-model management)
                          ├── Conversation memory (context persistence with TTL expiry)
                          ├── Document processing (PDF/Word/Markdown/HTML → Embedding)
                          ├── Model routing (multi-model switching, fallback, cost control)
                          ├── Observability (token stats, latency monitoring, Micrometer export)
                          ├── Resilience (retry → circuit breaker → fallback, three-tier protection)
                          └── Security (token-bucket rate limiting)
```

---

## Core Features

### Scenario Starters

| # | Scenario | Annotation | API Path | Description |
|---|----------|------------|----------|-------------|
| 1 | Chat | `@EnableChatAgent` | `/api/chat` | Multi-turn conversation + streaming + memory |
| 2 | RAG Q&A | `@EnableRagQa` | `/api/rag` | Document ETL → vector search → generation |
| 3 | Multi-Agent | `@EnableMultiAgent` | `/api/multi-agent` | Sequential / parallel / routing / debate modes |
| 4 | Code Review | `@EnableCodeReview` | `/api/code-review` | Git diff parsing + multi-dimension AI review |
| 5 | Data Analysis | `@EnableDataAnalysis` | `/api/data-analysis` | Natural language → SQL → AI analysis report |
| 6 | Customer Service | `@EnableCustomerService` | `/api/cs` | Intent classification + multi-turn + knowledge base |
| 7 | MCP Server | `@EnableMcp` | `/mcp/sse` | MCP protocol server, JSON-RPC + SSE |

### Infrastructure

- **🔀 Multi-Model Routing** — dynamic model switching, auto-selection by scenario / cost / latency, primary-fallback failover
- **🧠 Conversation Memory** — in-memory & Redis implementations, TTL expiry, scheduled cleanup, session list queries
- **📄 Document Processing** — load PDF / Word / Markdown / HTML / TXT; three chunking strategies (fixed-size / paragraph / semantic)
- **🎯 Skill System** — multi-source loading (classpath built-in + external directory + REST API dynamic registration), hot reload, semantic routing, file watcher — supports zero-downtime Skill modification in production
- **🛡️ Resilience** — retry (exponential backoff) → circuit breaker (Resilience4j) → fallback — three-tier protection
- **📊 Observability** — token tracking, latency monitoring, error rates, document / tool-call metrics, Micrometer → Prometheus / Grafana
- **🔒 Security** — token-bucket rate limiter at the API level

---

## Quick Start

### Prerequisites

- JDK 17+
- Maven 3.9+
- An OpenAI-compatible API key (MiMo, DashScope, OpenAI, Ollama, etc.)

> **Verified Environment**: All scenario starters have been verified with the Xiaomi MiMo model (`mimo-v2-pro`), passing 69 unit tests and 27 integration tests. Any model compatible with the OpenAI Chat Completions protocol works out of the box.

### 1. Add Dependencies

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.liziye</groupId>
            <artifactId>spring-ai-lab-bom</artifactId>
            <version>1.0.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <!-- Add the Chat scenario -->
    <dependency>
        <groupId>com.liziye</groupId>
        <artifactId>spring-ai-lab-scenario-chat</artifactId>
    </dependency>
</dependencies>
```

### 2. Configure `application.yml`

```yaml
spring:
  ai:
    dashscope:
      api-key: ${DASHSCOPE_API_KEY}
      base-url: https://your-api-endpoint.com/v1/chat/completions
      model: your-model-name
      max-tokens: 2048
      temperature: 0.7

    lab:
      memory:
        type: in-memory          # in-memory / redis
        max-history: 20           # max history messages
        ttl-minutes: 30           # session expiry
      observation:
        token-tracking: true
        latency-tracking: true
        metrics-export: true
```

### 3. Enable in One Step

As long as dependencies are on the classpath, Spring Boot's `AutoConfiguration.imports` mechanism auto-assembles everything. The `@EnableXxx` annotations are optional:

```java
// Approach A: no annotations needed — Spring Boot auto-assembles
@SpringBootApplication
public class MyApp {
    public static void main(String[] args) {
        SpringApplication.run(MyApp.class, args);
    }
}

// Approach B: explicit annotations for clarity (optional)
@SpringBootApplication
@EnableChatAgent
public class MyApp {
    public static void main(String[] args) {
        SpringApplication.run(MyApp.class, args);
    }
}
```

After startup, send a POST request to `http://localhost:8080/api/chat`:

```json
{
  "conversationId": "optional — auto-generated if omitted",
  "userInput": "Hello, introduce yourself"
}
```

### 4. Combine Multiple Scenarios

Add multiple scenario dependencies — all endpoints register automatically:

```xml
<dependencies>
    <dependency>
        <groupId>com.liziye</groupId>
        <artifactId>spring-ai-lab-scenario-chat</artifactId>
    </dependency>
    <dependency>
        <groupId>com.liziye</groupId>
        <artifactId>spring-ai-lab-scenario-rag</artifactId>
    </dependency>
    <dependency>
        <groupId>com.liziye</groupId>
        <artifactId>spring-ai-lab-scenario-code-review</artifactId>
    </dependency>
</dependencies>
```

Each scenario is independent and registers its own set of REST APIs.

---

## Integration Guide

Integrate Spring AI Lab scenario starters into your own Spring Boot project without writing a single line of Java code.

### How It Works

Every module in this project uses **Spring Boot 3.x standard `AutoConfiguration.imports` mechanism**. Each module's `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` file declares its auto-configuration class:

| Module | Registered Auto-Configuration |
|--------|------------------------------|
| `spring-ai-lab-core` | `LabAutoConfiguration` |
| `spring-ai-lab-scenario-chat` | `ChatAgentAutoConfiguration` |
| `spring-ai-lab-scenario-rag` | `RagQaAutoConfiguration` |
| `spring-ai-lab-scenario-multi-agent` | `MultiAgentAutoConfiguration` |
| `spring-ai-lab-scenario-code-review` | `CodeReviewAutoConfiguration` |
| `spring-ai-lab-scenario-data-analysis` | `DataAnalysisAutoConfiguration` |
| `spring-ai-lab-scenario-customer-service` | `CustomerServiceAutoConfiguration` |
| `spring-ai-lab-scenario-mcp` | `McpAutoConfiguration` |

**Once the jar is on the classpath, Spring Boot auto-scans and wires up these configuration classes at startup.** Controllers, services, agents, and all other beans are managed by the framework — no manual creation needed.

### Integration Steps

#### Step 1: Import BOM (Unified Version Management)

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.liziye</groupId>
            <artifactId>spring-ai-lab-bom</artifactId>
            <version>1.0.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

The BOM unifies versions not only for Spring AI Lab modules but also for third-party dependencies (Spring AI, Resilience4j, PDFBox, POI, etc.), preventing version conflicts in external projects.

#### Step 2: Add Scenario Dependencies as Needed

```xml
<dependencies>
    <!-- Just a chat feature -->
    <dependency>
        <groupId>com.liziye</groupId>
        <artifactId>spring-ai-lab-scenario-chat</artifactId>
    </dependency>

    <!-- RAG knowledge-base Q&A -->
    <dependency>
        <groupId>com.liziye</groupId>
        <artifactId>spring-ai-lab-scenario-rag</artifactId>
    </dependency>
</dependencies>
```

> `spring-ai-lab-core` is automatically included as a transitive dependency — no need to declare it explicitly.

#### Step 3: Configure API Key

Add to `application.yml`:

```yaml
spring:
  ai:
    dashscope:
      api-key: ${DASHSCOPE_API_KEY:your-api-key-here}
      base-url: https://your-endpoint.com/v1/chat/completions
      model: your-model-name
```

> Supports environment variable `DASHSCOPE_API_KEY` injection to avoid storing secrets in config files.

#### Step 4: Start and Call

Start your Spring Boot application. The following endpoints are automatically available:

| Dependency | Auto-Registered Endpoints |
|-----------|---------------------------|
| `spring-ai-lab-scenario-chat` | `POST /api/chat`, `POST /api/chat/stream`, `GET /api/chat/health` |
| `spring-ai-lab-scenario-rag` | `POST /api/rag/ask`, `POST /api/rag/ask/stream`, `POST /api/documents/upload` |
| `spring-ai-lab-scenario-multi-agent` | `POST /api/multi-agent/execute`, `POST /api/multi-agent/execute/stream` |
| `spring-ai-lab-scenario-code-review` | `POST /api/code-review/submit`, `POST /api/code-review/snippet` |
| `spring-ai-lab-scenario-data-analysis` | `POST /api/data-analysis/query`, `POST /api/data-analysis/generate-sql` |
| `spring-ai-lab-scenario-customer-service` | `POST /api/cs/chat`, `GET /api/cs/session/{id}/count` |
| `spring-ai-lab-scenario-mcp` | `GET /mcp/sse`, `POST /mcp/message` |

### About `@EnableXxx` Annotations

The `@EnableChatAgent`, `@EnableRagQa`, and similar annotations use `@Import` to explicitly bring in the corresponding `AutoConfiguration` class. Since the `AutoConfiguration.imports` file already does the same thing, **these annotations are not required under normal circumstances**. They primarily serve:

- **Fallback**: manually activate when `spring.boot.enableautoconfiguration=false`
- **Intent declaration**: make the code intent clearer by annotating the startup class

```java
// Both approaches are equivalent — pick one:

// Approach A: no annotations, relies on AutoConfiguration.imports
@SpringBootApplication
public class MyApp { }

// Approach B: explicit annotations (optional)
@SpringBootApplication
@EnableChatAgent
@EnableRagQa
public class MyApp { }
```

### Full Example

A typical external project needs only 3 files for AI chat + knowledge-base Q&A:

**① `pom.xml`**
```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.liziye</groupId>
            <artifactId>spring-ai-lab-bom</artifactId>
            <version>1.0.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>com.liziye</groupId>
        <artifactId>spring-ai-lab-scenario-chat</artifactId>
    </dependency>
    <dependency>
        <groupId>com.liziye</groupId>
        <artifactId>spring-ai-lab-scenario-rag</artifactId>
    </dependency>
</dependencies>
```

**② `application.yml`**
```yaml
spring:
  ai:
    dashscope:
      api-key: ${DASHSCOPE_API_KEY}
      base-url: https://token-plan-cn.xiaomimimo.com/v1/chat/completions
      model: mimo-v2-pro
    lab:
      memory:
        type: in-memory
        max-history: 20
        ttl-minutes: 30
```

**③ `MyApp.java`**
```java
@SpringBootApplication
public class MyApp {
    public static void main(String[] args) {
        SpringApplication.run(MyApp.class, args);
    }
}
```

After startup, `POST /api/chat` and `POST /api/rag/ask` are ready to use.

### Optional Dependencies

Some features require additional optional dependencies:

| Feature | Extra Dependency | Notes |
|---------|-----------------|-------|
| Redis Memory | `spring-boot-starter-data-redis` + Redis service | Set `memory.type` to `redis` |
| Document Processing | Auto-transitive (`spring-ai-lab-document`) | Already included in RAG scenario |
| Prometheus Export | `micrometer-registry-prometheus` | Used with `metrics-export: true` |

```xml
<!-- Switch to Redis memory -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

```yaml
spring:
  ai:
    lab:
      memory:
        type: redis
```

---

## Module Structure

### Project Overview

```
spring-ai-lab/
├── pom.xml                                    # Root POM (aggregation + version management)
│
├── spring-ai-lab-bom/                         # BOM — unified version control
│
├── spring-ai-lab-core/                        # Core abstractions (interfaces + common implementations)
│   ├── config/                                # Configuration property binding
│   ├── model/                                 # Common data models (AgentRequest/Response, etc.)
│   ├── memory/                                # Conversation memory (InMemory + Redis)
│   ├── orchestrator/                          # ★ Orchestration layer (BaseOrchestrator template method)
│   ├── routing/                               # Model routing (ModelRouter + ProviderManager)
│   ├── advisor/                               # Advisor enhancements (retry/fallback/logging/token)
│   ├── resilience/                            # Circuit breaker + fallback manager
│   ├── observation/                           # Observability (Micrometer metrics)
│   ├── security/                              # Security (token-bucket rate limiter)
│   ├── document/                              # Document interfaces (Loader/ChunkStrategy)
│   ├── exception/                             # Exception definitions + global exception handler
│   └── llm/                                   # LLM implementation (DashScopeChatModel)
│
├── spring-ai-lab-document/                    # Document processing implementation (standalone module)
│   ├── loader/                                # PDF / Word / Markdown / TXT / Web
│   └── chunk/                                 # Fixed-size / paragraph / semantic chunking
│
├── spring-ai-lab-scenario-chat/               # Scenario 1: Chat Agent
├── spring-ai-lab-scenario-rag/                # Scenario 2: RAG Knowledge-Base Q&A
├── spring-ai-lab-scenario-multi-agent/         # Scenario 3: Multi-Agent Collaboration
├── spring-ai-lab-scenario-code-review/         # Scenario 4: Code Review Assistant
├── spring-ai-lab-scenario-data-analysis/       # Scenario 5: Data Analysis NL2SQL
├── spring-ai-lab-scenario-customer-service/    # Scenario 6: Customer Service
├── spring-ai-lab-scenario-mcp/                # Scenario 7: MCP Server
│
├── spring-ai-lab-test/                        # Test utilities (Mock components + test base classes)
└── docs/                                      # Project documentation
```

### Module Dependency Graph

```
                       spring-ai-lab-bom (version management)
                              |
          ┌───────────────────┼───────────────────┐
          |                   |                   |
  spring-ai-lab-core   spring-ai-lab-*     spring-ai-lab-test
   (interfaces+abstractions) (scenario starters) (test utilities)
          |                   |
  spring-ai-lab-document      |
   (doc processing impl)      |
          |                   |
          └───────┬───────────┘
                  |
          Spring AI (Official)
```

**Key Design Notes**:

- `spring-ai-lab-document` is separated from Core — avoid pulling in heavyweight libraries (PDFBox, POI) when document processing is not needed
- All third-party model / vector store dependencies are marked `optional` — users pull them in as needed
- Scenario starters use `@ConditionalOnClass` conditional assembly to avoid `ClassNotFoundException`

---

## Architecture

Spring AI Lab adopts a five-layer architecture with unidirectional top-down dependency flow:

```
Scenario Starter Layer → Orchestration Layer → Capability Layer → Spring AI Base Layer → Infrastructure
```

### Scenario Starter Layer

End-user-facing "product" form. Each starter is activated via an `@EnableXxx` annotation and provides out-of-the-box HTTP APIs:

| Starter | Core Classes |
|---------|-------------|
| Chat | `ChatController` + `SimpleChatAgent` |
| RAG | `RagQaController` + `RagAgentOrchestrator` + `EtlPipeline` |
| Multi-Agent | `MultiAgentController` + `MultiAgentOrchestrator` |
| Code Review | `CodeReviewController` + `CodeReviewAgent` + `GitDiffParser` |
| Data Analysis | `DataAnalysisController` + `DataAnalysisAgent` + `SqlGenerator` |
| Customer Service | `CustomerServiceController` + `CustomerServiceOrchestrator` + `IntentClassifier` |
| MCP | `McpSseController` + `McpJsonRpcHandler` + `McpToolRegistry` |

### Orchestration Layer

The framework kernel, encapsulating orchestration logic common to all scenarios. The core design pattern is **Template Method**:

```
BaseOrchestrator<T extends AgentContext>
│
├── execute() defines the orchestration skeleton
│   ├── preProcess()      overridable, pre-process context
│   ├── doExecute()       ★ abstract, subclasses implement core logic
│   ├── postProcess()     overridable, post-process results
│   ├── updateMemory()    auto-saves conversation history
│   └── recordMetrics()   auto-records metrics
│
└── Handled automatically: memory | token stats | latency | exceptions | unified logging
```

Subclasses only need to implement `doExecute()` — their scenario-specific logic.

**Example: RAG scenario** (`RagAgentOrchestrator` extends `BaseOrchestrator<RagAgentContext>`):

1. Vector search (`VectorStore.similaritySearch`)
2. Assemble RAG prompt
3. Call ChatClient

Memory, metrics, and logging are handled automatically by the base class — subclasses don't need to worry about them.

### Capability Layer

Reusable horizontal technical capabilities, composed as needed:

| Capability | Components | Description |
|------------|-----------|-------------|
| Conversation Memory | `ConversationMemory` / `InMemoryConversationMemory` / `RedisConversationMemory` | Multi-turn context management, TTL expiry |
| Model Routing | `ModelProviderManager` + `ModelRouter` | Multi-model dynamic switching, primary-fallback failover |
| Retry & Fallback | `RetryAdvisor` + `FallbackAdvisor` + `CircuitBreakerManager` | Exponential backoff retry, circuit breaking, fallback |
| Document Processing | `DocumentLoader` + `ChunkStrategy` | PDF/Word/MD/HTML/TXT loading & chunking |
| Tool Registration | `ToolRegistry` | Implements Spring AI `ToolRegistrar` |
| Observability | `TokenMetrics` / `LatencyMetrics` / `ErrorMetrics` / `DocumentMetrics` / `ToolCallMetrics` | Micrometer metric export |

### Base Layer

100% based on Spring AI official APIs: `ChatClient`, `VectorStore`, `EmbeddingModel`, `ToolCallback`, `MCP Client`, etc. The framework does **not** monkey-patch or fork anything.

---

## API Reference

### Unified Response Format

All APIs use `ApiResult<T>` as a unified wrapper:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "content": "AI response content...",
    "conversationId": "conv_abc123",
    "toolCalls": [],
    "metadata": {
      "tokens": 1523,
      "latencyMs": 2300,
      "model": "mimo-v2-pro"
    },
    "fallback": false
  },
  "timestamp": "2026-05-27T10:30:00Z"
}
```

### All API Endpoints

| Scenario | Method | Path | Description |
|----------|--------|------|-------------|
| Chat | `POST` | `/api/chat` | Sync chat |
| Chat | `POST` | `/api/chat/stream` | Streaming chat (SSE) |
| Chat | `GET` | `/api/chat/health` | Health check |
| RAG | `POST` | `/api/rag/ask` | Knowledge-base Q&A |
| RAG | `POST` | `/api/rag/ask/stream` | Streaming knowledge-base Q&A |
| RAG | `GET` | `/api/rag/config` | Query RAG config |
| RAG | `POST` | `/api/documents/upload` | Upload document |
| RAG | `GET` | `/api/documents/progress` | Query document processing progress |
| Multi-Agent | `POST` | `/api/multi-agent/execute` | Multi-agent execution |
| Multi-Agent | `POST` | `/api/multi-agent/execute/stream` | Streaming multi-agent execution |
| Multi-Agent | `GET` | `/api/multi-agent/modes` | Query collaboration modes |
| Multi-Agent | `GET` | `/api/multi-agent/health` | Health check |
| Code Review | `POST` | `/api/code-review/submit` | Submit code review task |
| Code Review | `POST` | `/api/code-review/snippet` | Review code snippet |
| Code Review | `GET` | `/api/code-review/health` | Health check |
| Data Analysis | `POST` | `/api/data-analysis/query` | Natural language data query |
| Data Analysis | `POST` | `/api/data-analysis/generate-sql` | Generate SQL |
| Data Analysis | `GET` | `/api/data-analysis/schema` | Query data table schema |
| Data Analysis | `GET` | `/api/data-analysis/health` | Health check |
| Customer Service | `POST` | `/api/cs/chat` | Customer service chat |
| Customer Service | `GET` | `/api/cs/session/{id}/count` | Query session message count |
| MCP | `GET` | `/mcp/sse` | MCP SSE connection |
| MCP | `POST` | `/mcp/message` | MCP message handling |
| MCP | `GET` | `/mcp/sse/sessions/count` | Active session count |

### Streaming Responses (SSE)

Streaming endpoints return standard SSE event streams with the following event types:

| Event | Description |
|-------|-------------|
| `message` | AI response content chunk |
| `tool_call` | Tool invocation started |
| `tool_result` | Tool invocation result |
| `metadata` | Metadata summary (token / latency) |
| `done` | End-of-stream marker |

### Error Response

```json
{
  "code": 500,
  "message": "Model call failed: request timeout",
  "data": null,
  "error": {
    "type": "MODEL_TIMEOUT",
    "detail": "API request timed out after 3 retries, fallback triggered",
    "timestamp": "2026-05-27T10:30:00Z"
  }
}
```

---

## Configuration Reference

### Global Configuration

```yaml
spring:
  ai:
    dashscope:
      api-key: ${DASHSCOPE_API_KEY}         # API Key
      base-url: https://your-endpoint/v1/chat/completions
      model: your-model-name
      max-tokens: 2048
      temperature: 0.7
      connect-timeout: 30
      read-timeout: 120

    lab:
      # Memory Management
      memory:
        type: in-memory                      # in-memory / redis
        max-history: 20
        ttl-minutes: 30
        cleanup-interval-minutes: 60
        redis:                               # Redis mode config
          host: localhost
          port: 6379
          key-prefix: "ailab:memory:"

      # Model Routing
      model-group:
        default: primary                     # Default model group
        fallback: backup                     # Fallback model group

      # Resilience
      retry:
        enabled: true
        max-attempts: 3
        backoff-strategy: exponential        # fixed / exponential
        initial-delay-ms: 1000
        max-delay-ms: 10000
        multiplier: 2.0

      fallback:
        enabled: true
        fallback-response: "Sorry, the AI service is temporarily unavailable. Please try again later."

      circuit-breaker:
        enabled: true
        failure-rate-threshold: 50
        wait-duration-in-open-state: 60s
        sliding-window-size: 10

      # Observability
      observation:
        token-tracking: true
        latency-tracking: true
        metrics-export: true
        export-prefix: "ai_lab"

      # Security
      security:
        rate-limit:
          enabled: true
          chat:
            permits-per-second: 10
          rag:
            permits-per-second: 5

      # Skill System
      skill:
        enabled: true
        directory: skills                       # Built-in Skill classpath path (JAR fallback)
        external-dir: ./custom-skills           # External writable directory (production config)
        auto-init: true                         # Auto-copy built-in skills to external dir on first startup
        enable-management: false                # Enable REST API management endpoints
        hot-reload: true                        # Watch external dir for file changes and auto-refresh
        routing-strategy: semantic              # Routing strategy: semantic / keyword / llm
        similarity-threshold: 0.1               # Semantic similarity threshold (0-1)
        max-matched-skills: 3                   # Max skills matched per request
```

### Multi-Environment Configuration

```yaml
# application-dev.yml — Development
spring:
  ai:
    dashscope:
      base-url: http://localhost:11434/v1/chat/completions  # Local Ollama
      model: qwen2.5:7b
    lab:
      retry:
        max-attempts: 1
      circuit-breaker:
        enabled: false

# application-prod.yml — Production
spring:
  ai:
    dashscope:
      api-key: ${PROD_API_KEY}
    lab:
      memory:
        type: redis
      retry:
        max-attempts: 3
      circuit-breaker:
        enabled: true
```

---

## Observability

Spring AI Lab integrates Micrometer for Prometheus + Grafana monitoring.

### Built-in Metrics

| Category | Metric Name (Micrometer prefix `ai_lab_`) | Description |
|----------|-------------------------------------------|-------------|
| Token | `tokens_total`, `tokens_per_request`, `tokens_by_model` | Token consumption |
| Latency | `latency_seconds`, `latency_by_scenario` | Request latency distribution |
| Errors | `errors_total{type="..."}` | Error counts by type |
| Documents | `documents_loaded_total`, `vectors_stored_total`, `etl_duration_seconds` | ETL processing stats |
| Tool Calls | `tool_calls_total{tool="...", status="..."}`, `tool_call_duration_seconds` | Tool call stats |

### Grafana Dashboard

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
```

The Grafana Dashboard JSON configuration file is available at `docs/grafana-dashboard.json`.

---

## Tech Stack

| Category | Technology | Version |
|----------|-----------|---------|
| Framework | Spring Boot | 3.4.5 |
| AI Framework | Spring AI | 1.1.5 |
| JDK | Java | 17+ |
| Build | Maven | 3.9+ |
| Resilience | Resilience4j | 2.3.0 |
| Monitoring | Micrometer | 1.14.3 |
| PDF Parsing | Apache PDFBox | 3.0.4 |
| Word Parsing | Apache POI | 5.4.0 |
| Web Scraping | Jsoup | 1.18.1 |
| Git Operations | JGit | (optional) |
| Testing | JUnit 5 + Mockito | 5.14.2 |

---

## Contributing

Issues and Pull Requests are welcome!

### Development Workflow

1. Fork this repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Write code and pass tests (`mvn test`)
4. Commit changes (`git commit -m 'feat: add amazing feature'`)
5. Push to branch (`git push origin feature/amazing-feature`)
6. Create a Pull Request

### Code Style

- Follow JDK source Javadoc style: class comments include `@author` / `@since`; public methods include `@param` / `@return` / `@throws`
- Use Lombok to simplify code (`@Slf4j`, `@Data`, `@RequiredArgsConstructor`, etc.)
- All scenario starters extend `BaseOrchestrator`; subclasses only implement `doExecute()`
- Log format: `log.info("[MODULE] key1={} key2={}", v1, v2);`

---

## Author

**Li Ziye (liziye)**

---

> **Made with ❤️ for the Spring AI community**

---

## Changelog

### v0.3.0 (2026-05-31)

**Skill System Enhancement: Multi-Source Loading & REST API Management**

- 🎯 **Multi-Source Skill Loading**: three-layer priority loading — REST API dynamic registration > external file directory > classpath built-in; same-name Skills are overridden by higher priority
- 📁 **External Directory Support**: new `external-dir` config — users just specify a writable filesystem directory to add/modify Skill files anytime without rebuilding or redeploying JARs
- 🔄 **Auto Initialization**: new `auto-init` option — auto-copies built-in Skills from JAR to external directory on first startup; ideal for first-time production deployment
- 🌐 **REST API Management Endpoints**: new `SkillManageController` with 5 management endpoints (`GET/POST/DELETE /api/skills/**`, `POST /api/skills/reload`) for runtime CRUD Skill management
- 🗂️ **Built-in Skills Packaged in JAR**: moved built-in `.md` Skill files to `spring-ai-lab-core/src/main/resources/skills/` to serve as fallback source after publishing to private repository
- ⚙️ **New Config Options**: `external-dir`, `auto-init`, `enable-management` (see Configuration Reference)
- ✅ **Zero lint errors, all 42 unit tests passed**

### v0.2.0

- Initial scenario starters: Chat, RAG, Multi-Agent, Code Review, Data Analysis, Customer Service, MCP Server
- Infrastructure: multi-model routing, conversation memory, document processing, resilience, observability, rate limiting
