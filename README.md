# MyRAG — AI Chat + RAG 系统

基于 Spring AI Alibaba + Qdrant 混合检索的智能对话与知识库检索平台。

## 功能

- **AI Chat**：通义大模型对话，小模型智能路由 RAG 知识库
- **RAG 检索**：Qdrant dense + BM25 混合检索（RRF 融合）
- **MCP 服务**：对外暴露知识库检索工具
- **管理后台**：知识库/文档管理、召回测试、监控仪表盘
- **安全防护**：System Prompt 服务端固定，Prompt 注入检测

## 技术栈

- Java 21、Spring Boot 3.4、Spring AI Alibaba 1.1.2
- DashScope（通义）、Qdrant、MySQL、Redis
- React + Ant Design（管理后台）

## 快速开始

### 方式 A：Docker 一键运行（推荐）

```bash
cp .env.example .env   # 填入 AI_DASHSCOPE_API_KEY
docker compose up -d --build
# 管理后台: http://localhost:3000
```

详见 **[docs/deploy/docker-run.md](docs/deploy/docker-run.md)**。

### 方式 B：本地开发

```bash
docker compose -f docker-compose.infra.yml up -d
cp .env.example .env
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
export $(grep -v '^#' .env | xargs)
mvn -pl myrag-server spring-boot:run
cd admin-web && npm install && npm run dev
```

详见 **[docs/deploy/quickstart.md](docs/deploy/quickstart.md)**。

## 文档

| 文档 | 说明 |
|------|------|
| [docs/README.md](docs/README.md) | 文档中心索引 |
| [docs/deploy/docker-run.md](docs/deploy/docker-run.md) | Docker 一键运行 |
| [docs/deploy/local-dev.md](docs/deploy/local-dev.md) | 本地开发指南 |
| [docs/deploy/configuration.md](docs/deploy/configuration.md) | 环境变量与配置 |
| [docs/plan/README.md](docs/plan/README.md) | 架构与实施计划 |
| [docs/api/README.md](docs/api/README.md) | API 接口文档 |

## API 端点

| 端点 | 说明 |
|------|------|
| `POST /api/v1/chat` | 对话（非流式） |
| `POST /api/v1/chat/stream` | 对话（SSE 流式） |
| `POST /api/v1/rag/search` | RAG 检索 |
| `GET /api/v1/rag/knowledge-bases` | 知识库列表 |
| `GET /api/v1/admin/**` | 管理 API |
| `/mcp/message` | MCP 服务 |

## License

Apache 2.0
