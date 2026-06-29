# MCP 客户端接入

MyRAG 内置 MCP Server，对外暴露知识库检索工具，可在 Cursor、Claude Desktop 等 MCP 客户端中使用。

## 服务端配置

MCP 随 `myrag-server` 一起启动，默认配置（`application.yml`）：

```yaml
spring:
  ai:
    mcp:
      server:
        name: myrag-mcp
        version: 0.1.0
        protocol: STREAMABLE
        type: SYNC
```

| 项 | 值 |
|----|-----|
| 协议 | STREAMABLE（HTTP + SSE） |
| 端点 | `http://localhost:8080/mcp/message` |

## 暴露的工具

| 工具名 | 说明 | 参数 |
|--------|------|------|
| `searchKnowledgeBase` | 混合 RAG 检索 | `kbId`, `query`, `topK`（可选） |
| `listKnowledgeBases` | 列出可用知识库 | 无 |

## 在 Cursor 中接入

1. 确保 MyRAG 后端已启动（`mvn -pl myrag-server spring-boot:run`）
2. 打开 Cursor Settings → MCP
3. 添加 Server 配置：

```json
{
  "mcpServers": {
    "myrag": {
      "url": "http://localhost:8080/mcp/message"
    }
  }
}
```

4. 保存后，Cursor 应显示 `myrag-mcp` 已连接
5. 在对话中可调用 `searchKnowledgeBase` 检索知识库

### 使用示例

在 Cursor Agent 对话中：

> 请使用 myrag 的 listKnowledgeBases 查看有哪些知识库，然后 searchKnowledgeBase 搜索「退货政策」

## 在 Claude Desktop 中接入

编辑 Claude Desktop 配置文件：

- macOS: `~/Library/Application Support/Claude/claude_desktop_config.json`
- Windows: `%APPDATA%\Claude\claude_desktop_config.json`

```json
{
  "mcpServers": {
    "myrag": {
      "url": "http://localhost:8080/mcp/message"
    }
  }
}
```

重启 Claude Desktop 生效。

## 验证 MCP 服务

### 检查服务是否可达

```bash
curl -I http://localhost:8080/mcp/message
```

### 通过 REST API 间接验证工具依赖的数据

```bash
# 确认有知识库可供 MCP 工具列出
curl http://localhost:8080/api/v1/rag/knowledge-bases
```

## 常见问题

### MCP 连接显示 disconnected

1. 确认后端在 8080 端口运行
2. 确认 `spring.ai.mcp.server.protocol=STREAMABLE`
3. 查看后端日志是否有 MCP 相关错误

### searchKnowledgeBase 返回空

1. 确认已通过管理后台创建知识库并上传文档
2. 确认 `kbId` 正确（可通过 `listKnowledgeBases` 获取）
3. 在管理后台 **召回测试** 页面验证检索是否正常

### 远程访问

若 MCP 客户端与后端不在同一机器，需：

1. 将 `localhost` 改为后端实际 IP/域名
2. 确保防火墙放行 8080 端口
3. 生产环境建议加 HTTPS 反向代理（Nginx）和 API Key 认证
