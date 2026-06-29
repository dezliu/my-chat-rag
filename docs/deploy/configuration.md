# 配置说明

本文档说明 MyRAG 的环境变量与 `application.yml` 配置项。

## 环境变量

项目根目录 `.env.example` 列出了所有环境变量，复制为 `.env` 后修改：

```bash
cp .env.example .env
```

| 变量名 | 必填 | 默认值 | 说明 |
|--------|------|--------|------|
| `AI_DASHSCOPE_API_KEY` | **是** | — | 阿里云百炼 DashScope API Key |
| `SPRING_DATASOURCE_URL` | 否 | `jdbc:mysql://localhost:3306/myrag?...` | MySQL 连接 URL |
| `SPRING_DATASOURCE_USERNAME` | 否 | `myrag` | 数据库用户名 |
| `SPRING_DATASOURCE_PASSWORD` | 否 | `myrag` | 数据库密码 |
| `SPRING_DATA_REDIS_HOST` | 否 | `localhost` | Redis 主机 |
| `SPRING_DATA_REDIS_PORT` | 否 | `6379` | Redis 端口 |
| `SPRING_AI_VECTORSTORE_QDRANT_HOST` | 否 | `localhost` | Qdrant 主机 |
| `SPRING_AI_VECTORSTORE_QDRANT_PORT` | 否 | `6334` | Qdrant gRPC 端口 |

### 加载环境变量

```bash
# 当前 shell 生效
export $(grep -v '^#' .env | xargs)

# 或在启动命令前 inline
AI_DASHSCOPE_API_KEY=sk-xxx mvn -pl myrag-server spring-boot:run
```

> **安全提示**：`.env` 已在 `.gitignore` 中，切勿提交到 Git。

---

## application.yml 配置项

配置文件路径：`myrag-server/src/main/resources/application.yml`

### Spring AI / DashScope

```yaml
spring:
  ai:
    dashscope:
      api-key: ${AI_DASHSCOPE_API_KEY}
      chat:
        options:
          model: qwen-plus          # 主对话模型
      embedding:
        options:
          model: text-embedding-v3  # 向量嵌入模型
```

| 配置 | 说明 |
|------|------|
| `chat.options.model` | 主对话模型，默认 `qwen-plus` |
| `embedding.options.model` | Embedding 模型，默认 `text-embedding-v3`（1024 维） |

### Qdrant 向量库

```yaml
spring:
  ai:
    vectorstore:
      qdrant:
        host: localhost
        port: 6334    # gRPC 端口，不是 6333
```

### MCP Server

```yaml
spring:
  ai:
    mcp:
      server:
        name: myrag-mcp
        version: 0.1.0
        protocol: STREAMABLE   # HTTP SSE，供 Cursor/Claude 连接
        type: SYNC
```

### 业务配置（myrag.*）

```yaml
myrag:
  chat:
    router-model: qwen-turbo       # 路由小模型（低成本）
    chat-model: qwen-plus          # 主对话模型
    max-user-tokens: 4096          # 单条用户消息最大字符近似上限
    max-rag-context-tokens: 8192   # RAG 上下文最大字符近似上限
    default-system-prompt: "..."    # 默认 System Prompt（DB 无配置时使用）

  rag:
    hybrid:
      dense-limit: 20              # dense 检索 prefetch 数量
      sparse-limit: 20             # BM25 sparse 检索 prefetch 数量
      final-top-k: 5               # RRF 融合后返回数量
      fusion: rrf                    # 融合算法
      embedding-dimensions: 1024   # 须与 embedding 模型维度一致

  security:
    prompt-injection-enabled: true # 是否启用 Prompt 注入检测
```

### 修改 System Prompt

**方式一（推荐）**：管理后台 → 系统配置 → 编辑并保存

**方式二**：API

```bash
curl -X PUT http://localhost:8080/api/v1/admin/system-prompt \
  -H "Content-Type: application/json" \
  -d '{"prompt":"你是一个专业的客服助手..."}'
```

System Prompt 存储在 MySQL `system_prompt_config` 表，Chat API **不接受**客户端传入 system 字段。

### 知识库分块配置

创建知识库时可传入 `chunkConfigJson`：

```json
{
  "chunkSize": 500,
  "chunkOverlap": 50
}
```

| 字段 | 说明 | 默认 |
|------|------|------|
| `chunkSize` | 每个分块最大字符数 | 500 |
| `chunkOverlap` | 相邻分块重叠字符数 | 50 |

---

## 端口配置

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `server.port` | 8080 | 后端 HTTP 端口 |

修改端口：

```bash
mvn -pl myrag-server spring-boot:run -Dspring-boot.run.arguments="--server.port=9090"
```

---

## 日志配置

```yaml
logging:
  level:
    com.myrag: INFO
    org.springframework.ai: INFO
```

开发调试时可改为 `DEBUG`：

```bash
mvn -pl myrag-server spring-boot:run \
  -Dspring-boot.run.arguments="--logging.level.com.myrag=DEBUG"
```

---

## Actuator 端点

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

| 端点 | 说明 |
|------|------|
| `/actuator/health` | 健康检查 |
| `/actuator/metrics` | Micrometer 指标 |
| `/actuator/prometheus` | Prometheus 格式指标 |
