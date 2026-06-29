# AI Chat + RAG 系统实现计划

> 本文档为项目架构与实施计划，与 Cursor Plan 同步保存。

## 总体架构

采用 **Maven 多模块单体**，一个 Spring Boot 进程启动全部能力，模块边界清晰，后期可按模块拆分微服务。

- **客户端**：Admin Web (React)、Chat API Client、MCP Client (Cursor/Claude)
- **服务端模块**：myrag-chat、myrag-rag-query、myrag-rag-admin、myrag-rag-mcp、myrag-rag-monitor、myrag-rag-core
- **基础设施**：DashScope 通义模型、Qdrant 向量库、PostgreSQL 元数据、Redis 会话缓存

## 模块划分

| 模块 | 职责 |
|------|------|
| `myrag-common` | 公共 DTO、异常、常量、安全工具 |
| `myrag-rag-core` | Qdrant 混合检索、文档解析/分块/入库、Embedding |
| `myrag-rag-query` | 对外 REST 查询 API |
| `myrag-rag-admin` | 知识库/文档/配置 CRUD REST API |
| `myrag-rag-mcp` | MCP Server，暴露检索工具 |
| `myrag-rag-monitor` | 召回日志、指标、质量评估 |
| `myrag-chat` | Chat API、路由小模型、Prompt 防护 |
| `myrag-server` | 启动入口，聚合所有模块 |
| `admin-web` | React + Ant Design 管理后台 |

**技术栈：**
- Spring Boot 3.4.x、Java 21
- Spring AI Alibaba 1.1.2.x
- Qdrant + BM25 混合检索
- PostgreSQL + Flyway、Redis

## 核心数据模型（PostgreSQL）

- `knowledge_base` — 知识库
- `document` — 文档
- `document_chunk` — 分块元数据
- `recall_log` — 召回监控日志
- `recall_quality_alert` — 召回质量告警
- `system_prompt_config` — 系统 Prompt 配置
- `chat_session` / `chat_message` — 会话持久化

## RAG 混合检索

Spring AI 标准 VectorStore 仅支持 dense 检索，BM25 混合检索封装 Qdrant 原生 Universal Query API：

1. DashScope `text-embedding-v3` 生成 dense 向量
2. `Bm25SparseEncoder` 中文分词 + 稀疏向量编码
3. Qdrant prefetch dense + sparse，RRF 融合

## Chat 流程

1. PromptGuard — 注入检测 + 长度校验
2. RagRouter (qwen-turbo) — 判断是否需要 RAG、选择知识库
3. HybridSearch — 检索上下文
4. ChatClient (qwen-plus) — 生成回答（System Prompt 服务端固定）

## API 概览

### RAG Query
- `POST /api/v1/rag/search`
- `GET /api/v1/rag/knowledge-bases`

### Chat
- `POST /api/v1/chat`
- `POST /api/v1/chat/stream`

### Admin
- 知识库/文档 CRUD
- 系统 Prompt 管理
- 召回测试与监控

### MCP
- `searchKnowledgeBase`、`listKnowledgeBases`

## 实施阶段

### Phase 1 — 基础骨架
- Maven 多模块、docker-compose、Flyway、DashScope 连通

### Phase 2 — RAG 核心
- 混合检索、文档入库、Query/Admin API

### Phase 3 — Chat
- 路由小模型、PromptGuard、Chat API

### Phase 4 — MCP + 监控 + Admin UI
- MCP Server、召回监控、React 前端

## 本地开发

```bash
docker compose up -d
export AI_DASHSCOPE_API_KEY=your-key
./mvnw -pl myrag-server spring-boot:run
cd admin-web && npm install && npm run dev
```
