# Docker 基础设施

MyRAG 使用 Docker Compose 管理本地开发所需的基础设施（`docker-compose.infra.yml`），**后端和前端不在 Docker 中运行**，仅在本地直接启动。

完整应用 Docker 部署见 [docker-run.md](docker-run.md)。

## 服务清单

| 容器名 | 镜像 | 端口 | 用途 |
|--------|------|------|------|
| `myrag-mysql` | `mysql:8.4` | 3306 | 元数据（知识库、文档、日志、Prompt） |
| `myrag-redis` | `redis:7-alpine` | 6379 | 会话缓存 |
| `myrag-qdrant` | `qdrant/qdrant:v1.13.2` | 6333, 6334 | 向量存储（dense + sparse） |

## 常用命令

```bash
# 启动基础设施（后台）
docker compose -f docker-compose.infra.yml up -d

# 查看状态
docker compose -f docker-compose.infra.yml ps

# 查看日志
docker compose -f docker-compose.infra.yml logs -f
docker compose -f docker-compose.infra.yml logs -f mysql

# 停止
docker compose -f docker-compose.infra.yml down

# 停止并删除数据卷（清空所有数据，慎用）
docker compose -f docker-compose.infra.yml down -v

# 重启单个服务
docker compose -f docker-compose.infra.yml restart mysql
```

## MySQL

| 项 | 值 |
|----|-----|
| Host | `localhost` |
| Port | `3306` |
| Database | `myrag` |
| User | `myrag` |
| Password | `myrag` |
| Root Password | `myrag_root` |

连接命令：

```bash
docker exec -it myrag-mysql mysql -umyrag -pmyrag myrag
```

常用查询：

```sql
-- 查看知识库
SELECT id, name, status, collection_name FROM knowledge_base;

-- 查看文档
SELECT id, filename, status, chunk_count FROM document;

-- 查看召回日志
SELECT kb_id, query, result_count, latency_ms, created_at
FROM recall_log ORDER BY created_at DESC LIMIT 10;
```

数据持久化在 Docker volume `mysql_data`。

字符集：`utf8mb4`，排序规则：`utf8mb4_unicode_ci`。

## Redis

| 项 | 值 |
|----|-----|
| Host | `localhost` |
| Port | `6379` |

连接测试：

```bash
docker exec -it myrag-redis redis-cli ping
# 期望: PONG
```

数据持久化在 Docker volume `redis_data`。

## Qdrant

| 项 | 值 |
|----|-----|
| HTTP / Dashboard | http://localhost:6333 |
| gRPC（后端使用） | `localhost:6334` |

### Dashboard

浏览器访问 http://localhost:6333/dashboard 可：

- 查看所有 collection（每个知识库对应一个 `kb_xxx` collection）
- 查看 point 数量与 payload
- 手动执行搜索测试

### Collection 结构

每个知识库创建时自动生成 collection，包含：

- **dense 向量**：1024 维，Cosine 距离（DashScope text-embedding-v3）
- **sparse 向量**：BM25 稀疏向量，IDF modifier

数据持久化在 Docker volume `qdrant_data`。

## 健康检查

`docker-compose.infra.yml` 中每个服务都配置了 healthcheck：

```bash
docker compose -f docker-compose.infra.yml ps
```

若 MySQL 启动慢，后端首次连接可能失败，等待 healthy 后重启后端即可。

## 自定义配置

### 修改端口（避免冲突）

编辑 `docker-compose.infra.yml`：

```yaml
services:
  mysql:
    ports:
      - "13306:3306"   # 宿主机改用 13306
```

同步修改 `.env`：

```bash
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:13306/myrag?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=utf8
```

### 生产环境建议

本地 Compose 仅用于开发。生产环境建议：

- MySQL / Redis 使用云服务或独立部署，开启认证与 TLS
- Qdrant 使用集群模式或 Qdrant Cloud
- 修改默认密码（当前 `myrag/myrag` 仅供本地开发）
- 不要将 3306/6379/6333 端口暴露到公网
