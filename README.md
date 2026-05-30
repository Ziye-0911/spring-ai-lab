# Spring AI Lab

> 基于 Spring AI 的 AI 应用快速开发工具箱
>
> Spring AI Lab — AI scenario starters for Spring Boot. Chat, RAG, multi-agent, code review, and more — each available as a drop-in dependency with auto-configured REST APIs. Zero boilerplate, production-ready.
>
> **Spring AI Application Rapid Development Toolkit**

---

## 目录

- [项目简介](#项目简介)
- [核心特性](#核心特性)
- [快速开始](#快速开始)
- [外部接入指南](#外部接入指南)
- [模块结构](#模块结构)
- [架构设计](#架构设计)
- [API 参考](#api-参考)
- [配置参考](#配置参考)
- [可观测性](#可观测性)
- [技术栈](#技术栈)
- [贡献指南](#贡献指南)
- [作者](#作者)

---

## 项目简介

Spring AI Lab 是一套基于 Spring AI 的 AI 应用快速开发工具箱，将 AI 应用开发中反复出现的通用逻辑封装为即插即用的**场景模板**，让开发者关注业务而非基础设施。

### 设计原则

- **约定优于配置** — 每个场景模板默认就能跑，只需配 API Key
- **渐进式** — 可以只用一个 Starter，也可以组合多个
- **不重新发明轮子** — 底层 100% 基于 Spring AI 官方 API，只做组合和封装
- **可独立运行** — 每个场景模板是独立模块，不依赖其他场景
- **核心只定义抽象** — Core 模块不含重量级第三方库实现

### 与 Spring AI 的关系

```
Spring AI (官方)
├── ChatClient  ────┐
├── Tool Calling ───┤
├── RAG / Vector ───┤── Spring AI Lab (本项目)
├── MCP ────────────┤    │
└── Advisors ───────┘    ├── 场景模板 (组合官方能力为即用产品)
                          ├── 统一配置 (简化多模型管理)
                          ├── 对话记忆 (上下文持久化，支持 TTL 过期)
                          ├── 文档处理 (PDF/Word/Markdown/HTML → Embedding)
                          ├── 模型路由 (多模型切换、降级、成本控制)
                          ├── 可观测性 (Token 统计、延迟监控、Mircometer 导出)
                          ├── 容错降级 (重试 → 熔断 → 降级 三级防护)
                          └── 安全防护 (令牌桶限流)
```

---

## 核心特性

### 场景模板

| # | 场景 | 注解 | API 路径 | 说明 |
|---|------|------|----------|------|
| 1 | 通用对话 | `@EnableChatAgent` | `/api/chat` | 多轮对话 + 流式输出 + 记忆管理 |
| 2 | RAG 问答 | `@EnableRagQa` | `/api/rag` | 文档 ETL → 向量检索 → 生成回答 |
| 3 | 多 Agent 协作 | `@EnableMultiAgent` | `/api/multi-agent` | 顺序/并行/路由/辩论 四种协作模式 |
| 4 | 代码审查 | `@EnableCodeReview` | `/api/code-review` | Git Diff 解析 + 多维度 AI 审查 |
| 5 | 数据分析 | `@EnableDataAnalysis` | `/api/data-analysis` | 自然语言 → SQL → AI 分析报告 |
| 6 | 智能客服 | `@EnableCustomerService` | `/api/cs` | 意图识别 + 多轮对话 + 知识库 |
| 7 | MCP Server | `@EnableMcp` | `/mcp/sse` | MCP 协议服务端，JSON-RPC + SSE |

### 基础设施

- **🔀 多模型路由** — 支持多模型动态切换，按场景/成本/延迟自动选择，支持主备降级
- **🧠 对话记忆** — 内存/Redis 两种实现，支持 TTL 过期、定时清理、会话列表查询
- **📄 文档处理** — PDF/Word/Markdown/HTML/TXT 五格式加载，固定大小/段落/语义三种切分策略
- **🎯 Skill 系统** — 多源加载（classpath 内置 + 外部目录 + REST API 动态注册），热加载、语义路由、文件变更监听，支持生产环境零停机修改 Skill
- **🛡️ 容错降级** — 重试（指数退避）→ 熔断器（Resilience4j）→ 降级（Fallback Advisor）三级防护
- **📊 可观测性** — Token 统计、延迟监控、错误率、文档/工具调用指标，Mircometer → Prometheus/Grafana
- **🔒 安全防护** — 令牌桶限流器，接口级速率限制

---

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.9+
- 一个 OpenAI 兼容的 API Key（如 MiMo、DashScope、OpenAI 等）

> **验证环境**：本项目所有场景模板均通过小米 MiMo 模型（`mimo-v2-pro`）完成功能验证，包括 69 个单元测试和 27 个集成测试全部通过。只要你的模型兼容 OpenAI Chat Completions 协议，即可直接使用。

### 1. 添加依赖

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
    <!-- 引入通用对话场景 -->
    <dependency>
        <groupId>com.liziye</groupId>
        <artifactId>spring-ai-lab-scenario-chat</artifactId>
    </dependency>
</dependencies>
```

### 2. 配置 application.yml

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
        max-history: 20           # 最大历史消息数
        ttl-minutes: 30           # 会话过期时间
      observation:
        token-tracking: true
        latency-tracking: true
        metrics-export: true
```

### 3. 一键启用

只要依赖存在于 Classpath 上，Spring Boot 的 `AutoConfiguration.imports` 机制自动装配所有组件。`@EnableXxx` 注解是可选的显式声明：

```java
// 方式一：不写任何注解，Spring Boot 自动装配
@SpringBootApplication
public class MyApp {
    public static void main(String[] args) {
        SpringApplication.run(MyApp.class, args);
    }
}

// 方式二：加注解，意图更清晰（非必需）
@SpringBootApplication
@EnableChatAgent
public class MyApp {
    public static void main(String[] args) {
        SpringApplication.run(MyApp.class, args);
    }
}
```

启动后访问 `http://localhost:8080/api/chat`，发送 POST 请求即可开始对话：

```json
{
  "conversationId": "可选，不传则自动生成",
  "userInput": "你好，请介绍一下你自己"
}
```

### 4. 组合多个场景

同时引入多个场景依赖，所有端点自动注册：

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

每个场景互不干扰，各自注册独立的 REST API。

---

---

## 外部接入指南

无需编写任何 Java 代码即可将 Spring AI Lab 场景模板集成到你自己的 Spring Boot 项目中。

### 接入原理

本项目的每个模块都使用了 **Spring Boot 3.x 标准 `AutoConfiguration.imports` 机制**。每个模块的 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 文件申明了自动配置类：

| 模块 | 注册的自动配置类 |
|------|----------------|
| `spring-ai-lab-core` | `LabAutoConfiguration` |
| `spring-ai-lab-scenario-chat` | `ChatAgentAutoConfiguration` |
| `spring-ai-lab-scenario-rag` | `RagQaAutoConfiguration` |
| `spring-ai-lab-scenario-multi-agent` | `MultiAgentAutoConfiguration` |
| `spring-ai-lab-scenario-code-review` | `CodeReviewAutoConfiguration` |
| `spring-ai-lab-scenario-data-analysis` | `DataAnalysisAutoConfiguration` |
| `spring-ai-lab-scenario-customer-service` | `CustomerServiceAutoConfiguration` |
| `spring-ai-lab-scenario-mcp` | `McpAutoConfiguration` |

**一旦 jar 进入 Classpath，Spring Boot 启动时会自动扫描并装配这些配置类。** 控制器 (Controller)、服务、Agent 等 Bean 全由框架管理，无需业务方手动创建。

### 接入步骤

#### 第 1 步：引入 BOM（统一版本管理）

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

BOM 不仅统一管理 Spring AI Lab 自身的模块版本，还统一了 Spring AI、Resilience4j、PDFBox、POI 等第三方依赖的版本，避免外部项目出现版本冲突。

#### 第 2 步：按需引入场景依赖

```xml
<dependencies>
    <!-- 只需一个对话功能 -->
    <dependency>
        <groupId>com.liziye</groupId>
        <artifactId>spring-ai-lab-scenario-chat</artifactId>
    </dependency>

    <!-- 需要 RAG 知识库问答 -->
    <dependency>
        <groupId>com.liziye</groupId>
        <artifactId>spring-ai-lab-scenario-rag</artifactId>
    </dependency>
</dependencies>
```

> `spring-ai-lab-core` 会自动作为传递依赖引入，无需显式声明。

#### 第 3 步：配置 API Key

在 `application.yml` 中添加：

```yaml
spring:
  ai:
    dashscope:
      api-key: ${DASHSCOPE_API_KEY:your-api-key-here}
      base-url: https://your-endpoint.com/v1/chat/completions
      model: your-model-name
```

> 支持通过环境变量 `DASHSCOPE_API_KEY` 注入，避免密钥写入配置文件。

#### 第 4 步：启动并调用

启动你的 Spring Boot 应用，无需编写任何 Java 代码。以下端点自动可用：

| 引入的依赖 | 自动注册的端点 |
|-----------|---------------|
| `spring-ai-lab-scenario-chat` | `POST /api/chat`、`POST /api/chat/stream`、`GET /api/chat/health` |
| `spring-ai-lab-scenario-rag` | `POST /api/rag/ask`、`POST /api/rag/ask/stream`、`POST /api/documents/upload` |
| `spring-ai-lab-scenario-multi-agent` | `POST /api/multi-agent/execute`、`POST /api/multi-agent/execute/stream` |
| `spring-ai-lab-scenario-code-review` | `POST /api/code-review/submit`、`POST /api/code-review/snippet` |
| `spring-ai-lab-scenario-data-analysis` | `POST /api/data-analysis/query`、`POST /api/data-analysis/generate-sql` |
| `spring-ai-lab-scenario-customer-service` | `POST /api/cs/chat`、`GET /api/cs/session/{id}/count` |
| `spring-ai-lab-scenario-mcp` | `GET /mcp/sse`、`POST /mcp/message` |

### 关于 `@EnableXxx` 注解

项目中的 `@EnableChatAgent`、`@EnableRagQa` 等注解通过 `@Import` 显式引入对应的 `AutoConfiguration` 类。由于 `AutoConfiguration.imports` 文件已经做了同样的事，**这些注解在正常情况下不是必需的**。它们主要用于以下场景：

- **兜底方案**：当 `spring.boot.enableautoconfiguration=false` 时手动激活
- **意图声明**：在启动类上添加注解，让代码意图更加明确

```java
// 以下两种写法等价，二者选一即可：

// 写法 A：不加注解，依赖 AutoConfiguration.imports 自动装配
@SpringBootApplication
public class MyApp { }

// 写法 B：显式声明注解（可选，非必需）
@SpringBootApplication
@EnableChatAgent
@EnableRagQa
public class MyApp { }
```

### 完整示例

一个典型的外部项目，只需 3 个文件即可拥有 AI 对话 + 知识库问答能力：

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

启动后即可调用 `POST /api/chat` 和 `POST /api/rag/ask` 等端点。

### 可选依赖

某些能力需要额外可选依赖：

| 能力 | 额外依赖 | 说明 |
|------|---------|------|
| Redis 记忆 | `spring-boot-starter-data-redis` + Redis 服务 | memory.type 切换为 redis |
| 文档处理 | 自动传递（`spring-ai-lab-document`） | RAG 场景已包含 |
| Prometheus 导出 | `micrometer-registry-prometheus` | 配合 `metrics-export: true` |

```xml
<!-- 切换到 Redis 记忆 -->
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

## 模块结构

### 项目总览

```
spring-ai-lab/
├── pom.xml                                    # 根 POM（聚合 + 统一版本管理）
│
├── spring-ai-lab-bom/                         # BOM — 统一版本控制
│
├── spring-ai-lab-core/                        # 核心抽象（接口 + 通用实现）
│   ├── config/                                # 配置属性绑定
│   ├── model/                                 # 通用数据模型（AgentRequest/Response 等）
│   ├── memory/                                # 对话记忆（InMemory + Redis 实现）
│   ├── orchestrator/                          # ★ 编排层（BaseOrchestrator 模板方法）
│   ├── routing/                               # 模型路由（ModelRouter + ProviderManager）
│   ├── advisor/                               # Advisor 增强（重试/降级/日志/Token）
│   ├── resilience/                            # 熔断器 + 降级管理器
│   ├── observation/                           # 可观测性（Mircometer 指标）
│   ├── security/                              # 安全（令牌桶限流）
│   ├── document/                              # 文档接口（Loader/ChunkStrategy）
│   ├── exception/                             # 异常定义 + 全局异常处理
│   └── llm/                                   # LLM 实现（DashScopeChatModel）
│
├── spring-ai-lab-document/                    # 文档处理实现（独立模块）
│   ├── loader/                                # PDF / Word / Markdown / TXT / Web
│   └── chunk/                                 # 固定大小 / 段落 / 语义切分
│
├── spring-ai-lab-scenario-chat/               # 场景 1：通用对话 Agent
├── spring-ai-lab-scenario-rag/                # 场景 2：RAG 知识库问答
├── spring-ai-lab-scenario-multi-agent/         # 场景 3：多 Agent 协作
├── spring-ai-lab-scenario-code-review/         # 场景 4：代码审查助手
├── spring-ai-lab-scenario-data-analysis/       # 场景 5：数据分析 NL2SQL
├── spring-ai-lab-scenario-customer-service/    # 场景 6：智能客服
├── spring-ai-lab-scenario-mcp/                # 场景 7：MCP Server
│
├── spring-ai-lab-test/                        # 测试工具（Mock 组件 + 测试基类）
└── docs/                                      # 项目文档
```

### 模块依赖关系

```
                       spring-ai-lab-bom (版本管理)
                              |
          ┌───────────────────┼───────────────────┐
          |                   |                   |
  spring-ai-lab-core   spring-ai-lab-*     spring-ai-lab-test
   (仅接口+抽象)        (各场景模板)        (测试工具)
          |                   |
  spring-ai-lab-document      |
   (文档处理实现)              |
          |                   |
          └───────┬───────────┘
                  |
          Spring AI (官方)
```

**关键设计说明**：

- `spring-ai-lab-document` 从 Core 独立 — 无文档处理需求时不引入 pdfbox/poi 等重量级依赖
- 所有第三方模型/向量库依赖标记 `optional`，用户按需引入
- 场景模板通过 `@ConditionalOnClass` 条件装配，避免 `ClassNotFoundException`

---

## 架构设计

Spring AI Lab 采用五层架构，依赖自上而下单向流动：

```
场景模板层 → 编排层 → 能力层 → Spring AI 基础层 → 基础设施
```

### 场景模板层

面向最终用户的"产品"形态。每个模板通过 `@EnableXxx` 注解一键激活，提供开箱即用的 HTTP API：

| 模板 | 核心类 |
|------|--------|
| Chat | `ChatController` + `SimpleChatAgent` |
| RAG | `RagQaController` + `RagAgentOrchestrator` + `EtlPipeline` |
| Multi-Agent | `MultiAgentController` + `MultiAgentOrchestrator` |
| Code Review | `CodeReviewController` + `CodeReviewAgent` + `GitDiffParser` |
| Data Analysis | `DataAnalysisController` + `DataAnalysisAgent` + `SqlGenerator` |
| Customer Service | `CustomerServiceController` + `CustomerServiceOrchestrator` + `IntentClassifier` |
| MCP | `McpSseController` + `McpJsonRpcHandler` + `McpToolRegistry` |

### 编排层

框架内核，封装所有场景共用的编排逻辑。核心设计模式为**模板方法模式**：

```
BaseOrchestrator<T extends AgentContext>
│
├── execute() 定义编排骨架
│   ├── preProcess()      子类可覆盖，预处理上下文
│   ├── doExecute()       ★ 抽象方法，子类实现核心逻辑
│   ├── postProcess()     子类可覆盖，后处理结果
│   ├── updateMemory()    自动保存对话历史
│   └── recordMetrics()   自动记录指标
│
└── 自动处理：记忆管理 | Token 统计 | 延迟监控 | 异常兜底 | 统一日志
```

子类只需实现 `doExecute()` 中的场景差异化逻辑。

**以 RAG 场景为例**（`RagAgentOrchestrator` 继承 `BaseOrchestrator<RagAgentContext>`）：

1. 向量检索（`VectorStore.similaritySearch`）
2. 组装 RAG Prompt
3. 调用 ChatClient

记忆、指标、日志由基类自动处理，子类无需关心。

### 能力层

可复用的横向技术能力，按需组合：

| 能力 | 组件 | 说明 |
|------|------|------|
| 对话记忆 | `ConversationMemory` / `InMemoryConversationMemory` / `RedisConversationMemory` | 多轮对话上下文管理，TTL 过期 |
| 模型路由 | `ModelProviderManager` + `ModelRouter` | 多模型动态切换、主备降级 |
| 重试降级 | `RetryAdvisor` + `FallbackAdvisor` + `CircuitBreakerManager` | 指数退避重试、熔断、降级 |
| 文档处理 | `DocumentLoader` + `ChunkStrategy` | PDF/Word/MD/HTML/TXT 加载与切分 |
| 工具注册 | `ToolRegistry` | 实现 Spring AI `ToolRegistrar` 接口 |
| 可观测性 | `TokenMetrics` / `LatencyMetrics` / `ErrorMetrics` / `DocumentMetrics` / `ToolCallMetrics` | Mircometer 指标导出 |

### 基础层

100% 基于 Spring AI 官方 API：`ChatClient`、`VectorStore`、`EmbeddingModel`、`ToolCallback`、`MCP Client` 等。框架不做任何魔改或 Fork。

---

## API 参考

### 统一响应格式

所有 API 使用 `ApiResult<T>` 统一包装：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "content": "AI 的回复内容...",
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

### 所有 API 端点

| 场景 | 方法 | 路径 | 说明 |
|------|------|------|------|
| Chat | `POST` | `/api/chat` | 同步对话 |
| Chat | `POST` | `/api/chat/stream` | 流式对话 (SSE) |
| Chat | `GET` | `/api/chat/health` | 健康检查 |
| RAG | `POST` | `/api/rag/ask` | 知识库问答 |
| RAG | `POST` | `/api/rag/ask/stream` | 流式知识库问答 |
| RAG | `GET` | `/api/rag/config` | 查询 RAG 配置 |
| RAG | `POST` | `/api/documents/upload` | 上传文档 |
| RAG | `GET` | `/api/documents/progress` | 查询文档处理进度 |
| Multi-Agent | `POST` | `/api/multi-agent/execute` | 多 Agent 协作执行 |
| Multi-Agent | `POST` | `/api/multi-agent/execute/stream` | 流式多 Agent 协作 |
| Multi-Agent | `GET` | `/api/multi-agent/modes` | 查询协作模式 |
| Multi-Agent | `GET` | `/api/multi-agent/health` | 健康检查 |
| Code Review | `POST` | `/api/code-review/submit` | 提交代码审查任务 |
| Code Review | `POST` | `/api/code-review/snippet` | 审查代码片段 |
| Code Review | `GET` | `/api/code-review/health` | 健康检查 |
| Data Analysis | `POST` | `/api/data-analysis/query` | 自然语言数据查询 |
| Data Analysis | `POST` | `/api/data-analysis/generate-sql` | 生成 SQL |
| Data Analysis | `GET` | `/api/data-analysis/schema` | 查询数据表结构 |
| Data Analysis | `GET` | `/api/data-analysis/health` | 健康检查 |
| Customer Service | `POST` | `/api/cs/chat` | 客服对话 |
| Customer Service | `GET` | `/api/cs/session/{id}/count` | 查询会话消息数 |
| MCP | `GET` | `/mcp/sse` | MCP SSE 连接 |
| MCP | `POST` | `/mcp/message` | MCP 消息处理 |
| MCP | `GET` | `/mcp/sse/sessions/count` | 活跃会话数 |

### 流式响应 (SSE)

流式接口返回标准 SSE 事件流，包含以下事件类型：

| event | 说明 |
|-------|------|
| `message` | AI 回复内容片段 |
| `tool_call` | 工具调用开始 |
| `tool_result` | 工具调用结果 |
| `metadata` | 元数据汇总（Token/延迟） |
| `done` | 流结束标记 |

### 错误响应

```json
{
  "code": 500,
  "message": "模型调用失败: 请求超时",
  "data": null,
  "error": {
    "type": "MODEL_TIMEOUT",
    "detail": "API 请求超时，已尝试重试 3 次后触发降级",
    "timestamp": "2026-05-27T10:30:00Z"
  }
}
```

---

## 配置参考

### 全局配置

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
      # 记忆管理
      memory:
        type: in-memory                      # in-memory / redis
        max-history: 20
        ttl-minutes: 30
        cleanup-interval-minutes: 60
        redis:                               # Redis 模式配置
          host: localhost
          port: 6379
          key-prefix: "ailab:memory:"

      # 模型路由
      model-group:
        default: primary                     # 默认模型组
        fallback: backup                     # 降级模型组

      # 容错
      retry:
        enabled: true
        max-attempts: 3
        backoff-strategy: exponential        # fixed / exponential
        initial-delay-ms: 1000
        max-delay-ms: 10000
        multiplier: 2.0

      fallback:
        enabled: true
        fallback-response: "抱歉，AI 服务暂时不可用，请稍后重试。"

      circuit-breaker:
        enabled: true
        failure-rate-threshold: 50
        wait-duration-in-open-state: 60s
        sliding-window-size: 10

      # 可观测性
      observation:
        token-tracking: true
        latency-tracking: true
        metrics-export: true
        export-prefix: "ai_lab"

      # 安全
      security:
        rate-limit:
          enabled: true
          chat:
            permits-per-second: 10
          rag:
            permits-per-second: 5

      # Skill 系统
      skill:
        enabled: true
        directory: skills                       # 内置 Skill classpath 路径（JAR 内兜底）
        external-dir: ./custom-skills           # 外部可写目录（生产环境核心配置）
        auto-init: true                         # 首次启动时自动将内置 Skill 复制到外部目录
        enable-management: false                # 是否启用 REST API 管理端点
        hot-reload: true                        # 监听外部目录文件变更自动刷新
        routing-strategy: semantic              # 路由策略：semantic / keyword / llm
        similarity-threshold: 0.1               # 语义匹配相似度阈值（0-1）
        max-matched-skills: 3                   # 每次请求最多匹配的 Skill 数
```

### 多环境配置

```yaml
# application-dev.yml — 开发环境
spring:
  ai:
    dashscope:
      base-url: http://localhost:11434/v1/chat/completions  # 本地 Ollama
      model: qwen2.5:7b
    lab:
      retry:
        max-attempts: 1
      circuit-breaker:
        enabled: false

# application-prod.yml — 生产环境
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

## 可观测性

Spring AI Lab 集成 Micrometer，支持 Prometheus + Grafana 监控。

### 内置指标

| 指标类别 | 指标名（Mircometer 前缀 `ai_lab_`） | 说明 |
|----------|-------------------------------------|------|
| Token | `tokens_total`, `tokens_per_request`, `tokens_by_model` | Token 消耗统计 |
| 延迟 | `latency_seconds`, `latency_by_scenario` | 请求延迟分布 |
| 错误 | `errors_total{type="..."}` | 按错误类型统计 |
| 文档 | `documents_loaded_total`, `vectors_stored_total`, `etl_duration_seconds` | ETL 处理统计 |
| 工具调用 | `tool_calls_total{tool="...", status="..."}`, `tool_call_duration_seconds` | 工具调用统计 |

### Grafana Dashboard

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
```

Grafana Dashboard JSON 配置文件见 `docs/grafana-dashboard.json`。

---

## 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| 基础框架 | Spring Boot | 3.4.5 |
| AI 框架 | Spring AI | 1.1.5 |
| JDK | Java | 17+ |
| 构建 | Maven | 3.9+ |
| 容错 | Resilience4j | 2.3.0 |
| 监控 | Micrometer | 1.14.3 |
| PDF 解析 | Apache PDFBox | 3.0.4 |
| Word 解析 | Apache POI | 5.4.0 |
| 网页抓取 | Jsoup | 1.18.1 |
| Git 操作 | JGit | (optional) |
| 测试 | JUnit 5 + Mockito | 5.14.2 |

---

## 贡献指南

欢迎提交 Issue 和 Pull Request！

### 开发流程

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/amazing-feature`)
3. 编写代码并通过测试 (`mvn test`)
4. 提交变更 (`git commit -m 'feat: add amazing feature'`)
5. 推送到分支 (`git push origin feature/amazing-feature`)
6. 创建 Pull Request

### 代码规范

- 遵循 JDK 源码 Javadoc 风格：类注释含 `@author` / `@since`，公共方法含 `@param` / `@return` / `@throws`
- 使用 Lombok 简化代码（`@Slf4j`、`@Data`、`@RequiredArgsConstructor` 等）
- 所有场景模板通过 `BaseOrchestrator` 继承，子类只实现 `doExecute()` 方法
- 日志格式：`log.info("[MODULE] key1={} key2={}", v1, v2);`

---

## 作者

**李子叶 (liziye)**

---

> **Made with ❤️ for the Spring AI community**

---

## 版本更新

### v0.3.0 (2026-05-30)

**Skill 系统增强：多源加载与 REST API 管理**

- 🎯 **多源 Skill 加载**：支持三层优先级加载 —— REST API 动态注册 > 外部文件目录 > classpath 内置，同名 Skill 高优先级覆盖低优先级
- 📁 **外部目录支持**：新增 `external-dir` 配置，用户只需指定一个可写文件系统目录即可随时新增/修改 Skill 文件，无需重新打包部署
- 🔄 **自动初始化**：新增 `auto-init` 选项，首次启动时自动将 JAR 内置 Skill 复制到外部目录，适合生产环境首次部署
- 🌐 **REST API 管理端点**：新增 `SkillManageController`，提供 5 个管理端点（`GET/POST/DELETE /api/skills/**`、`POST /api/skills/reload`），支持运行时 CRUD 管理 Skill
- 🗂️ **内置 Skill 打包进 JAR**：将内置 Skill `.md` 文件复制到 `spring-ai-lab-core/src/main/resources/skills/`，确保发布到私服后仍可作为兜底来源
- ⚙️ **新增配置项**：`external-dir`、`auto-init`、`enable-management`（详见配置参考）
- ✅ **零 lint 错误，全部 42 个单元测试通过**

### v0.2.0

- 初始场景模板：Chat、RAG、Multi-Agent、Code Review、Data Analysis、Customer Service、MCP Server
- 基础设施：多模型路由、对话记忆、文档处理、容错降级、可观测性、安全限流
