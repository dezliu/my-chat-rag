# MyRAG 文档中心

本目录存放项目的设计、部署与使用文档，按主题分子目录管理。

## 目录结构

| 目录 | 说明 |
|------|------|
| [plan/](plan/) | 架构设计、模块划分、实施计划 |
| [deploy/](deploy/) | 环境要求、本地启动、配置、Docker、MCP 接入 |
| [api/](api/) | REST API 与 MCP 工具说明 |

## 快速导航

### 两种运行方式

| 方式 | 适用场景 | 文档 |
|------|----------|------|
| **Docker 一键运行（推荐）** | 快速体验、演示、不想装 Java/Node | [deploy/docker-run.md](deploy/docker-run.md) |
| **本地开发** | 改代码、断点调试 | [deploy/quickstart.md](deploy/quickstart.md) |

### 其他文档

- 部署文档索引 → [deploy/README.md](deploy/README.md)
- 详细本地开发 → [deploy/local-dev.md](deploy/local-dev.md)
- 环境变量与配置 → [deploy/configuration.md](deploy/configuration.md)
- Docker 基础设施 → [deploy/docker.md](deploy/docker.md)
- 架构与模块说明 → [plan/README.md](plan/README.md)
- API 接口列表 → [api/README.md](api/README.md)

## 服务端口一览

| 服务 | 端口 | 说明 |
|------|------|------|
| 后端 API | 8080 | Spring Boot 主服务 |
| 管理前端 | 3000 | Docker: Nginx / 本地: Vite dev |
| MySQL | 3306 | 元数据存储 |
| Redis | 6379 | 会话缓存 |
| Qdrant HTTP | 6333 | 向量库 REST / Dashboard |
| Qdrant gRPC | 6334 | 向量库 gRPC（后端使用） |

## Docker 快速命令

```bash
# 完整应用（5 容器）
cp .env.example .env && docker compose up -d --build

# 仅基础设施（3 容器，本地开发用）
docker compose -f docker-compose.infra.yml up -d
```
