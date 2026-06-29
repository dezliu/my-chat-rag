# 部署与运行文档

本目录包含 MyRAG 的环境要求、启动步骤、配置说明与 Docker 部署指南。

## 文档列表

| 文档 | 说明 |
|------|------|
| [docker-run.md](docker-run.md) | **Docker 一键运行完整应用（推荐）** |
| [quickstart.md](quickstart.md) | 本地开发模式（基础设施 Docker + 宿主机跑代码） |
| [local-dev.md](local-dev.md) | 本地开发详细步骤、验证与常见问题 |
| [configuration.md](configuration.md) | 环境变量、`application.yml` 配置项说明 |
| [docker.md](docker.md) | Docker 基础设施（MySQL/Redis/Qdrant）说明 |
| [mcp-client.md](mcp-client.md) | 在 Cursor / Claude 中接入 MCP 服务 |

## 两种启动方式

### 方式 A：Docker 一键运行（推荐，零本地依赖）

只需 Docker + DashScope API Key，**不需要**安装 Java / Maven / Node.js。

```bash
cp .env.example .env          # 填入 AI_DASHSCOPE_API_KEY
docker compose up -d --build  # 启动 6 个容器
# 管理后台: http://localhost:3000
# 用户 H5:   http://localhost:3001
```

详见 **[docker-run.md](docker-run.md)**。

### 方式 B：本地开发（改代码、调试）

基础设施在 Docker，后端/前端在宿主机运行。需要 Java 21、Maven、Node.js。

```bash
docker compose -f docker-compose.infra.yml up -d   # 仅 MySQL + Redis + Qdrant
cp .env.example .env
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
export $(grep -v '^#' .env | xargs)
mvn -pl myrag-server spring-boot:run               # 终端 1
cd admin-web && npm install && npm run dev         # 终端 2

# 用户 H5（新终端）
cd chat-h5 && npm install && npm run dev           # 终端 3，http://localhost:3001
```

详见 **[quickstart.md](quickstart.md)**。

## Compose 文件对照

| 文件 | 启动的服务 | 用途 |
|------|-----------|------|
| `docker-compose.yml` | mysql + redis + qdrant + **server** + **admin-web** + **chat-h5**（6 个） | Docker 完整部署 |
| `docker-compose.infra.yml` | mysql + redis + qdrant（3 个） | 本地开发基础设施 |

## 最小启动清单

**Docker 一键运行：**

- [ ] Docker Desktop 已运行
- [ ] 已获取 [DashScope API Key](https://bailian.console.aliyun.com/)
- [ ] 已配置 `.env` 中的 `AI_DASHSCOPE_API_KEY`

**本地开发额外需要：**

- [ ] Java 21 已安装
- [ ] Maven 3.9+ 已安装
- [ ] Node.js 18+ 已安装
