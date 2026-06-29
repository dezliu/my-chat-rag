# Docker 一键运行

本文档说明如何使用 Docker Compose **启动完整 MyRAG 应用**（基础设施 + 后端 + 管理前端）。

## 架构

```
浏览器 :3000
    │
    ▼
admin-web (Nginx)  ── /api ──►  server (Spring Boot :8080)
                                      │
                    ┌─────────────────┼─────────────────┐
                    ▼                 ▼                 ▼
               mysql               redis              qdrant
               :3306               :6379           :6333/:6334
```

容器内通过 Docker 网络服务名互联（`mysql`、`redis`、`qdrant`），无需改 `application.yml`。

---

## 前置条件

- Docker Desktop 或 Docker Engine + Docker Compose v2
- 阿里云百炼 **DashScope API Key**

---

## 三步启动

### 1. 配置 API Key

```bash
cd /path/to/myrag
cp .env.example .env
```

编辑 `.env`，填入：

```bash
AI_DASHSCOPE_API_KEY=sk-你的密钥
```

### 2. 构建并启动

```bash
docker compose up -d --build
```

> 若之前用过旧版 PostgreSQL 数据卷，首次切换 MySQL 时建议先执行 `docker compose down -v` 清空旧数据。

首次构建需下载 Maven / Node 依赖，**约 5～15 分钟**（视网络而定）。

### 3. 查看状态

```bash
docker compose ps
```

所有服务 `STATUS` 应为 `healthy` 或 `running`：

| 容器 | 说明 |
|------|------|
| `myrag-mysql` | MySQL 8.4 |
| `myrag-redis` | Redis |
| `myrag-qdrant` | 向量库 |
| `myrag-server` | 后端 API |
| `myrag-admin-web` | 管理前端 |
| `myrag-chat-h5` | 用户 H5 聊天 |

---

## 访问地址

| 服务 | 地址 |
|------|------|
| **管理后台** | http://localhost:3000 |
| **用户 H5 聊天** | http://localhost:3001 |
| 后端 API | http://localhost:8080 |
| 健康检查 | http://localhost:8080/actuator/health |
| Qdrant Dashboard | http://localhost:6333/dashboard |

---

## 验证

```bash
# 健康检查
curl http://localhost:8080/actuator/health

# 创建知识库（示例）
curl -X POST http://localhost:8080/api/v1/admin/knowledge-bases \
  -H "Content-Type: application/json" \
  -d '{"name":"Docker测试库","description":"Docker环境验证"}'
```

浏览器打开 http://localhost:3000 使用管理界面。

---

## 常用命令

```bash
# 查看日志（全部）
docker compose logs -f

# 仅后端日志
docker compose logs -f server

# 重新构建并启动（代码变更后）
docker compose up -d --build

# 仅重建后端
docker compose up -d --build server

# 停止
docker compose down

# 停止并删除数据（清空数据库与向量，慎用）
docker compose down -v
```

---

## 环境变量

Compose 从项目根目录 `.env` 读取变量。必填项：

| 变量 | 说明 |
|------|------|
| `AI_DASHSCOPE_API_KEY` | DashScope API Key（**必填**，需在 `.env` 中配置） |

其余连接信息已在 `docker-compose.yml` 的 `server.environment` 中写死为容器内地址，一般无需修改。

也可在命令行临时传入：

```bash
AI_DASHSCOPE_API_KEY=sk-xxx docker compose up -d --build
```

---

## 两种 Compose 文件

| 文件 | 用途 |
|------|------|
| `docker-compose.yml` | **完整应用**（推荐 Docker 部署） |
| `docker-compose.infra.yml` | 仅 MySQL + Redis + Qdrant（本地 IDE 开发后端/前端时用） |

本地开发（后端在宿主机跑）：

```bash
docker compose -f docker-compose.infra.yml up -d
# 然后 mvn -pl myrag-server spring-boot:run
```

---

## 故障排查

### `AI_DASHSCOPE_API_KEY is required`

未配置 `.env` 或未 export。确认：

```bash
grep AI_DASHSCOPE_API_KEY .env
```

### `server` 一直 restarting

```bash
docker compose logs server --tail 100
```

常见原因：
- API Key 无效
- 数据库未 ready（等待 mysql healthy 后 compose 会自动重试）

### 前端 502 / 无法访问 API

确认 `myrag-server` 已 healthy：

```bash
docker compose ps server
curl http://localhost:8080/actuator/health
```

### 构建失败镜像拉取 401 / EOF / timeout（国内网络）

错误示例：

```
failed to resolve source metadata for docker.io/library/node:20-alpine
... docker.m.daocloud.io ... 401 Unauthorized
```

或直连 Docker Hub：

```
Head "https://registry-1.docker.io/v2/...": EOF
context deadline exceeded
```

**原因：**
- 配置了失效的镜像加速（如 `docker.m.daocloud.io`）→ 401
- 清空 `registry-mirrors` 后仍无法访问 Docker Hub → EOF / 超时（国内常见）

**方式一（推荐）：Docker Desktop 配置可用加速**

Settings → Docker Engine，设置：

```json
{
  "registry-mirrors": [
    "https://docker.1ms.run"
  ]
}
```

Apply 重启后执行 `docker compose up -d --build`。

**方式二：项目 `.env` 指定镜像地址**

在 `.env` 中取消注释（加速源可用 `docker.1ms.run`、`docker.1panel.live` 等，以实际可拉取为准）：

```bash
DOCKER_IMAGE_PREFIX=docker.1ms.run/library/
MYSQL_IMAGE=docker.1ms.run/library/mysql:8.4
REDIS_IMAGE=docker.1ms.run/library/redis:7-alpine
QDRANT_IMAGE=docker.1ms.run/qdrant/qdrant:v1.13.2
```

然后：

```bash
docker compose build --no-cache
docker compose up -d
```

**验证：**

```bash
docker info | grep -A3 "Registry Mirrors"
docker pull docker.1ms.run/library/nginx:1.27-alpine
```

### 构建失败 `mvn package`

```bash
docker compose build server --no-cache
```

确保网络可访问 Maven Central；公司内网需配置 Docker 构建代理或镜像。

### 端口被占用

修改 `docker-compose.yml` 端口映射，例如：

```yaml
server:
  ports:
    - "9080:8080"
admin-web:
  ports:
    - "3001:80"
```

---

## 生产环境提示

当前 Compose 面向**本地 / 内网演示**，生产建议：

- 使用外部托管的 MySQL、Redis、Qdrant
- 通过 Nginx / Ingress 配置 HTTPS
- 使用 Docker Secrets 或 K8s Secret 管理 API Key
- 修改默认数据库密码
- 为 `server` 配置 JVM 内存：在 `Dockerfile` 或 compose 中加 `JAVA_OPTS=-Xms512m -Xmx1g`

更多配置见 [configuration.md](configuration.md)。
