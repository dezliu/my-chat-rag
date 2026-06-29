# 快速启动（本地开发模式）

本文档帮助你在本地以 **本地开发模式** 跑通 MyRAG：基础设施在 Docker，后端和前端在宿主机运行。

> 若希望 **全部在 Docker 里一键运行**（无需安装 Java/Node），请直接看 [docker-run.md](docker-run.md)。

## 两种模式对比

| | Docker 一键运行 | 本地开发（本文档） |
|--|----------------|-------------------|
| 文档 | [docker-run.md](docker-run.md) | 本文档 |
| Compose 文件 | `docker-compose.yml` | `docker-compose.infra.yml` |
| 需要 Java 21 | 否 | **是** |
| 需要 Node.js | 否 | **是**（管理前端 + 用户 H5） |
| 启动命令 | `docker compose up -d --build` | 见下方分步说明 |

---

## 前置条件

| 依赖 | 版本要求 | 检查命令 |
|------|----------|----------|
| Java | **21**（必须） | `java -version` |
| Maven | 3.9+ | `mvn -version` |
| Node.js | 18+ | `node -v` |
| Docker | 最新稳定版 | `docker compose version` |

> 管理后台与用户 H5 均需 Node.js；若只用 curl 测 API，可跳过前端步骤。

> Spring Boot 3.4 不支持 Java 8/11/17，必须使用 Java 21。

### 安装 Java 21（macOS Homebrew）

```bash
brew install openjdk@21
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
```

建议将上述 `export` 写入 `~/.zshrc`。

### 获取 DashScope API Key

1. 登录 [阿里云百炼控制台](https://bailian.console.aliyun.com/)
2. 创建 API Key
3. 记录 Key，后续填入 `.env`

---

## 第一步：进入项目目录

```bash
cd /path/to/myrag
```

## 第二步：启动基础设施（MySQL + Redis + Qdrant）

```bash
docker compose -f docker-compose.infra.yml up -d
```

等待容器 healthy：

```bash
docker compose -f docker-compose.infra.yml ps
```

应看到 **3 个**服务：`myrag-mysql`、`myrag-redis`、`myrag-qdrant`。

> 注意：不要用 `docker compose up -d`（不带 `-f`），那会启动**完整应用栈**（含后端和前端容器），见 [docker-run.md](docker-run.md)。

## 第三步：配置环境变量

```bash
cp .env.example .env
```

编辑 `.env`，至少修改：

```bash
AI_DASHSCOPE_API_KEY=sk-xxxxxxxxxxxxxxxx
```

加载环境变量：

```bash
export $(grep -v '^#' .env | xargs)
```

## 第四步：启动后端

```bash
# 确保 Java 21
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"

# 编译并启动
mvn -pl myrag-server spring-boot:run
```

看到以下日志表示启动成功：

```
Started MyragApplication in X.XXX seconds
```

验证健康检查：

```bash
curl http://localhost:8080/actuator/health
# 期望: {"status":"UP"}
```

## 第五步：启动管理前端

**新开一个终端**（保持后端终端运行）：

```bash
cd admin-web
npm install    # 首次需要，之后可跳过
npm run dev
```

启动成功后终端会显示：

```
  VITE vX.X.X  ready in XXX ms
  ➜  Local:   http://localhost:3000/
```

### 访问管理后台

浏览器打开：**http://localhost:3000**

| 页面 | 路径 | 用途 |
|------|------|------|
| 知识库管理 | 侧边栏「知识库」 | 创建 / 编辑知识库 |
| 文档管理 | 侧边栏「文档」 | 上传 `.txt` `.md` `.pdf` `.docx` |
| 召回测试 | 侧边栏「召回测试」 | 输入问题，查看检索结果 |
| 监控 | 侧边栏「监控」 | 召回日志与质量指标 |
| 系统配置 | 侧边栏「系统配置」 | System Prompt 等 |

前端通过 Vite 代理将 `/api` 请求转发到 `http://localhost:8080`，无需单独配置 CORS。

## 第六步：启动用户 H5 聊天页

### 方式 A：本地开发（npm run dev）

**再开一个终端**（保持后端与管理前端终端运行）：

```bash
cd chat-h5
npm install    # 首次需要，之后可跳过
npm run dev
```

启动成功后终端会显示：

```
  ▲ Next.js 14.x.x
  - Local:        http://localhost:3001
```

### 访问用户 H5

浏览器打开：**http://localhost:3001**

页面功能：

| 功能 | 说明 |
|------|------|
| 对话输入框 | 底部输入问题，Enter 发送，Shift+Enter 换行 |
| 流式回复 | 调用 `POST /api/v1/chat/stream`，逐字显示回答 |
| 停止生成 | 流式输出过程中可点击「停止」 |
| 新对话 | 右上角按钮，重置会话并清空消息 |
| 知识库引用 | 后端启用 RAG 时，回复气泡显示「已引用知识库」 |

H5 通过 Next.js rewrites 将 `/api` 代理到 `http://localhost:8080`。`sessionId` 自动保存在浏览器 `localStorage`，刷新页面不会丢失当前会话。

> 使用 H5 对话前，建议先在管理后台创建知识库并上传文档（见第七步）。

### 方式 B：Docker 单独启动 H5（后端已在 Docker 中运行时）

若你已通过 `docker compose up` 启动了后端与管理后台，但 **http://localhost:3001 打不开**，通常是 `chat-h5` 容器未启动（例如栈是在加入 H5 服务之前拉起的）。

**检查当前状态：**

```bash
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
curl -s -o /dev/null -w "3000: %{http_code}\n" http://localhost:3000/
curl -s -o /dev/null -w "3001: %{http_code}\n" http://localhost:3001/
curl -s http://localhost:8080/actuator/health
```

| 检查项 | 正常表现 |
|--------|----------|
| `myrag-server` | 容器存在，8080 返回 `{"status":"UP"}` |
| `myrag-admin-web` | 容器存在，3000 返回 `200` |
| `myrag-chat-h5` | 容器存在，3001 返回 `200` |
| 仅 3001 失败 | 说明 H5 未启动，执行下方命令 |

**只构建并启动 H5（不影响已在运行的其他容器）：**

```bash
cd /path/to/myrag
docker compose up -d --build chat-h5
```

验证：

```bash
docker ps | grep chat-h5
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:3001/
# 期望: 200
```

浏览器访问：**http://localhost:3001**

> 完整 Docker 部署说明见 [docker-run.md](docker-run.md)。若希望六个容器一并重建：`docker compose up -d --build`。

---

## 第七步：验证功能

### 1. 创建知识库

管理后台 → **知识库管理** → **新建知识库**

或使用 curl：

```bash
curl -X POST http://localhost:8080/api/v1/admin/knowledge-bases \
  -H "Content-Type: application/json" \
  -d '{"name":"测试知识库","description":"产品FAQ文档"}'
```

### 2. 上传文档

管理后台 → **文档管理** → 选择知识库 → **上传文档**

支持 `.txt`、`.md`、`.pdf`、`.docx`。

### 3. 召回测试

管理后台 → **召回测试** → 输入问题 → **检索**

### 4. AI 对话（curl）

```bash
curl -X POST http://localhost:8080/api/v1/chat \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": "test-session-001",
    "message": "你好，请介绍一下知识库中的内容"
  }'
```

### 5. H5 页面对话

1. 确认 **http://localhost:3001** 已打开且后端健康（`curl http://localhost:8080/actuator/health`）
2. 在输入框输入问题，点击「发送」或按 Enter
3. 观察流式输出；若知识库有相关内容，回复下方会出现「已引用知识库」标签
4. 点击「新对话」可开始新的会话

---

## 服务地址汇总

| 服务 | 地址 |
|------|------|
| 后端 API | http://localhost:8080 |
| 管理后台（Vite dev） | http://localhost:3000 |
| 用户 H5（Next.js dev） | http://localhost:3001 |
| 用户 H5（Docker） | http://localhost:3001（需 `docker compose up -d chat-h5`） |
| MySQL | localhost:3306 |
| Qdrant Dashboard | http://localhost:6333/dashboard |
| Actuator 健康检查 | http://localhost:8080/actuator/health |
| MCP 端点 | http://localhost:8080/mcp/message |

---

## 常见问题

| 现象 | 可能原因 | 处理 |
|------|----------|------|
| **http://localhost:3001 打不开** | `chat-h5` 未启动 | Docker：`docker compose up -d --build chat-h5`；本地：`cd chat-h5 && npm run dev` |
| 3000 能访问，3001 不行 | 仅管理后台在跑，H5 需单独启动 | 见上 |
| H5 页面能开，发消息失败 | 后端未就绪或 API Key 未配置 | `curl http://localhost:8080/actuator/health`；检查 `.env` 中 `AI_DASHSCOPE_API_KEY` |
| 本地 `npm run dev` 报端口占用 | 3001 已被占用 | `lsof -i :3001` 查看并结束占用进程 |
| Docker 构建 H5 很慢 | 首次需下载 Node 依赖并 build Next.js | 耐心等待，或改用本地 dev 模式（第六步 方式 A） |

---

## 停止服务

```bash
# 本地 dev：停止后端 / 管理前端 / H5 — 在各终端按 Ctrl+C

# Docker：停止 H5 容器
docker compose stop chat-h5

# Docker：停止完整应用栈
docker compose down

# 停止基础设施（本地开发模式）
docker compose -f docker-compose.infra.yml down

# 停止并清除数据卷（慎用，会删除数据库和向量数据）
docker compose -f docker-compose.infra.yml down -v
```

---

## 下一步

- **Docker 一键运行** → [docker-run.md](docker-run.md)
- 详细开发说明 → [local-dev.md](local-dev.md)
- 配置项说明 → [configuration.md](configuration.md)
- MCP 接入 Cursor → [mcp-client.md](mcp-client.md)
- API 接口文档 → [../api/README.md](../api/README.md)
