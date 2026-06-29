# 本地开发指南

本文档面向日常开发，涵盖编译、调试、模块说明与常见问题排查。

## 开发环境架构

```
┌─────────────────────────────────────────────────────────┐
│  admin-web (Vite :3000)                                  │
│  代理 /api → localhost:8080                              │
└────────────────────────┬────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────┐
│  myrag-server (Spring Boot :8080)                        │
│  ├── myrag-chat          Chat + 路由 + PromptGuard      │
│  ├── myrag-rag-query     RAG 查询 API                    │
│  ├── myrag-rag-admin     管理 API                        │
│  ├── myrag-rag-mcp       MCP Server                      │
│  ├── myrag-rag-monitor   召回监控                        │
│  └── myrag-rag-core      混合检索 + 文档入库             │
└──────┬──────────────┬──────────────┬────────────────────┘
       │              │              │
  MySQL            Qdrant         Redis
  :5432            :6333/:6334    :6379
       │              │
       └────── DashScope API（通义大模型，云端）
```

## 编译

### 全量编译

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
mvn clean compile
```

### 仅编译 server 及其依赖

```bash
mvn -pl myrag-server -am compile
```

### 打包 JAR

```bash
mvn -pl myrag-server -am package -DskipTests
java -jar myrag-server/target/myrag-server-0.1.0-SNAPSHOT.jar
```

## 启动方式

### 方式一：Maven 直接运行（推荐开发）

```bash
export $(grep -v '^#' .env | xargs)
mvn -pl myrag-server spring-boot:run
```

### 方式二：IDE 运行

1. 用 IntelliJ IDEA 打开项目根目录（导入 Maven 项目）
2. 设置 Project SDK 为 **Java 21**
3. 运行主类：`com.myrag.server.MyragApplication`
4. 在 Run Configuration 中配置 Environment Variables（从 `.env` 复制）

### 方式三：打包后运行

```bash
mvn -pl myrag-server -am package -DskipTests
export $(grep -v '^#' .env | xargs)
java -jar myrag-server/target/myrag-server-0.1.0-SNAPSHOT.jar
```

## 前端开发

```bash
cd admin-web
npm install
npm run dev        # 开发模式，http://localhost:3000
npm run build      # 生产构建，输出到 admin-web/dist/
npm run preview    # 预览生产构建
```

### 前端页面说明

| 路由 | 页面 | 功能 |
|------|------|------|
| `/knowledge-bases` | 知识库管理 | CRUD、分块参数 |
| `/documents` | 文档管理 | 上传、删除、查看索引状态 |
| `/recall-test` | 召回测试 | 实时 hybrid 检索验证 |
| `/monitor` | 监控仪表盘 | 召回量、延迟、质量告警 |
| `/system-config` | 系统配置 | System Prompt 编辑 |

## 数据库迁移

Flyway 在启动时自动执行 `myrag-server/src/main/resources/db/migration/` 下的 SQL。

新增迁移文件命名规范：`V2__description.sql`、`V3__add_column.sql`。

查看迁移状态：

```bash
# 连接 MySQL
docker exec -it myrag-mysql mysql -umyrag -pmyrag myrag -e "SELECT * FROM flyway_schema_history;"
```

## 调试技巧

### 查看 Qdrant 集合

浏览器打开 http://localhost:6333/dashboard ，可查看已创建的知识库 collection 及 point 数量。

### 查看召回日志

```bash
curl "http://localhost:8080/api/v1/admin/monitor/recall-logs?page=0&size=10"
```

### 调整日志级别

在 `application.yml` 或启动参数中：

```bash
mvn -pl myrag-server spring-boot:run \
  -Dspring-boot.run.arguments="--logging.level.com.myrag=DEBUG"
```

### 仅测试 RAG 检索（跳过 Chat）

```bash
curl -X POST http://localhost:8080/api/v1/rag/search \
  -H "Content-Type: application/json" \
  -d '{
    "kbIds": ["<知识库ID>"],
    "query": "你的问题",
    "topK": 5
  }'
```

## 常见问题

### 1. `class file has wrong version 61.0, should be 52.0`

**原因**：使用了 Java 8 编译，Spring Boot 3.4 需要 Java 21。

**解决**：

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
java -version   # 应显示 21.x
```

### 2. `Connection refused: localhost:5432`

**原因**：MySQL 容器未启动。

**解决**：

```bash
docker compose -f docker-compose.infra.yml up -d mysql
docker compose ps   # 确认 healthy
```

### 3. `AI_DASHSCOPE_API_KEY` 相关错误

**原因**：未设置 API Key 或 Key 无效。

**解决**：检查 `.env` 并 `export $(grep -v '^#' .env | xargs)`。

### 4. 文档上传后 status 为 FAILED

**可能原因**：
- DashScope Embedding API 调用失败（检查 Key 配额）
- 文档内容为空或格式不支持
- Qdrant 未启动

**排查**：查看后端日志中的 `Document indexing failed` 错误详情。

### 5. 前端请求 404 或 CORS 错误

**原因**：后端未启动，或端口不是 8080。

**解决**：确认后端运行在 8080，前端 dev 模式会自动代理 `/api`。

### 6. MCP 连接失败

参见 [mcp-client.md](mcp-client.md)。

## 模块修改指南

| 想改什么 | 改哪个模块 |
|----------|-----------|
| 混合检索逻辑 | `myrag-rag-core` → `HybridSearchService` |
| 文档分块策略 | `myrag-rag-core` → `TextSplitter`、`DocumentIngestionService` |
| Chat 路由规则 | `myrag-chat` → `RagRouterService` |
| Prompt 注入规则 | `myrag-chat` → `PromptGuardService` |
| 管理 API | `myrag-rag-admin` → `AdminController` |
| MCP 工具 | `myrag-rag-mcp` → `RagMcpTools` |
| 监控指标 | `myrag-rag-monitor` → `RecallLogService` |
| 全局配置 | `myrag-server` → `application.yml` |
