# API 接口文档

MyRAG 提供 **REST** 与 **GraphQL** 两种 API 入口：

| 入口 | 地址 | 说明 |
|------|------|------|
| REST | `http://localhost:8080/api/v1/*` | Spring Boot 原生接口 |
| GraphQL | `http://localhost:4000/graphql` | Apollo Server BFF，转发 REST（推荐） |

GraphQL 方案与迁移阶段见 [plan/apollo-graphql-bff.md](../plan/apollo-graphql-bff.md)。

REST API 统一返回格式：

```json
{
  "code": 0,
  "message": "success",
  "data": { ... }
}
```

GraphQL 成功时返回 `data`；业务错误在 `errors[].extensions.code` 中映射 REST 的 `code`。

---

## GraphQL API（Apollo BFF）

端点：`POST http://localhost:4000/graphql`  
Sandbox：`http://localhost:4000/graphql`（浏览器）

### 健康检查

```graphql
query { health }
```

### 知识库列表

```graphql
query {
  knowledgeBases {
    id
    name
    description
    status
    createdAt
  }
}
```

### 创建知识库

```graphql
mutation {
  createKnowledgeBase(input: { name: "测试库", description: "说明" }) {
    id
    name
  }
}
```

### AI 对话（非流式）

```graphql
mutation Chat($input: ChatInput!) {
  chat(input: $input) {
    sessionId
    reply
    usedRag
    cacheHit
    routeDecision {
      needRag
      reason
    }
  }
}
```

变量：

```json
{
  "input": {
    "sessionId": "test-001",
    "message": "你好"
  }
}
```

### 仍使用 REST 的能力

- 文档上传：`POST /api/v1/admin/knowledge-bases/{kbId}/documents`（multipart）
- SSE 流式聊天：`POST /api/v1/chat/stream`
- MCP、Actuator

---

## Chat API（REST）

### POST /api/v1/chat

非流式对话。

**请求体：**

```json
{
  "sessionId": "user-session-001",
  "message": "请问退货政策是什么？"
}
```

**响应：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "sessionId": "user-session-001",
    "reply": "...",
    "usedRag": true,
    "ragKbIds": ["kb-id-xxx"],
    "routeDecision": {
      "needRag": true,
      "kbIds": ["kb-id-xxx"],
      "reason": "用户询问退货政策",
      "confidence": 0.92
    }
  }
}
```

### POST /api/v1/chat/stream

SSE 流式对话，请求体同上，响应为 `text/event-stream`。

```bash
curl -N -X POST http://localhost:8080/api/v1/chat/stream \
  -H "Content-Type: application/json" \
  -d '{"sessionId":"s1","message":"你好"}'
```

---

## RAG Query API

### POST /api/v1/rag/search

混合检索（dense + BM25 RRF）。

**请求体：**

```json
{
  "kbIds": ["kb-id-xxx"],
  "query": "退货流程",
  "topK": 5,
  "minScore": 0.0
}
```

**响应：**

```json
{
  "code": 0,
  "data": {
    "query": "退货流程",
    "results": [
      {
        "content": "退货需在7天内...",
        "score": 0.85,
        "docId": "doc-id",
        "kbId": "kb-id-xxx",
        "metadata": { "pointId": "..." }
      }
    ],
    "latencyMs": 120
  }
}
```

### GET /api/v1/rag/knowledge-bases

列出所有 ACTIVE 状态的知识库。

---

## Admin API

前缀：`/api/v1/admin`

### 知识库

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/knowledge-bases` | 列表 |
| POST | `/knowledge-bases` | 创建 `{name, description, chunkConfigJson?}` |
| PUT | `/knowledge-bases/{id}` | 更新 |
| DELETE | `/knowledge-bases/{id}` | 软删除 |

### 文档

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/knowledge-bases/{kbId}/documents` | 列表 |
| POST | `/knowledge-bases/{kbId}/documents` | 上传（multipart `file`） |
| DELETE | `/documents/{docId}` | 删除 |
| POST | `/documents/{docId}/reindex` | 重新索引 |

### 系统 Prompt

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/system-prompt` | 获取当前 Prompt |
| PUT | `/system-prompt` | 更新 `{prompt: "..."}` |

### AI 配置

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/ai-config` | 获取 AI 配置（含 `provider`、`baseUrl`、`embeddingDimensions`、Key 脱敏、模型名等） |
| PUT | `/ai-config` | 更新 `{provider?, baseUrl?, embeddingDimensions?, apiKey?, routerModel, chatModel, embeddingModel, customChatModels?, customRouterModels?}`；`apiKey` 留空或不传表示不修改 |

GET 返回示例：`provider`（`dashscope`/`zhipuai`）、`baseUrl`、`embeddingDimensions`、`apiKeyMasked`、`apiKeyConfigured`、`apiKeySource`（`db`/`env`）、`routerModel`、`chatModel`、`embeddingModel`、`customChatModels`、`customRouterModels`。

### 召回测试

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/recall-test` | 同 `/rag/search`，额外触发质量评估 |

### 监控

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/monitor/recall-logs?kbId=&page=&size=` | 召回日志分页 |
| GET | `/monitor/chat-logs?page=&size=` | 用户问题日志（含缓存/RAG/质量分） |
| GET | `/monitor/cache-logs?page=&size=` | 缓存访问日志（历史） |
| GET | `/monitor/metrics` | 聚合指标（召回 + 用户提问 + 缓存） |
| DELETE | `/monitor/cache` | 清空答案缓存 |
| GET | `/monitor/alerts` | 质量告警 |

`GET /monitor/metrics` 返回字段包括：`totalRecalls`、`totalChatQueries`、`chatRagRate`、`chatRecallRate`、`cacheHitRate`、`avgQualityScore` 等。

---

## MCP 工具

协议：STREAMABLE HTTP，端点 `http://localhost:8080/mcp/message`

| 工具 | 参数 | 返回 |
|------|------|------|
| `searchKnowledgeBase` | `kbId`, `query`, `topK?` | 格式化检索结果文本 |
| `listKnowledgeBases` | 无 | 知识库 ID / 名称 / 描述列表 |

接入方式见 [deploy/mcp-client.md](../deploy/mcp-client.md)。

---

## Actuator

| 端点 | 说明 |
|------|------|
| GET `/actuator/health` | 健康检查 |
| GET `/actuator/metrics` | 指标列表 |
| GET `/actuator/prometheus` | Prometheus 格式指标 |
