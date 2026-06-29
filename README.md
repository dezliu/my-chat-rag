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
- DashScope（通义）、Qdrant、PostgreSQL、Redis
- React + Ant Design（管理后台）

## 快速开始

### 1. 启动基础设施

```bash
docker compose up -d
```

### 2. 配置环境变量

```bash
cp .env.example .env
# 编辑 .env，填入 AI_DASHSCOPE_API_KEY
export $(cat .env | xargs)
```

### 3. 启动后端

需要 **Java 21**：

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home  # macOS Homebrew
export AI_DASHSCOPE_API_KEY=your-key
./mvnw -pl myrag-server spring-boot:run
```

### 4. 启动管理前端

```bash
cd admin-web
npm install
npm run dev
```

## API 端点

| 端点 | 说明 |
|------|------|
| `POST /api/v1/chat` | 对话（非流式） |
| `POST /api/v1/chat/stream` | 对话（SSE 流式） |
| `POST /api/v1/rag/search` | RAG 检索 |
| `GET /api/v1/rag/knowledge-bases` | 知识库列表 |
| `GET /api/v1/admin/**` | 管理 API |
| `/mcp/message` | MCP 服务 |

## 项目结构

详见 [docs/PLAN.md](docs/PLAN.md)

## License

Apache 2.0
