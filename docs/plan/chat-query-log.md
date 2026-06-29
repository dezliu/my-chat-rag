# 用户问题日志与监控

> 统一记录每次 Chat 请求，管理后台可查看路由、缓存、RAG、召回与质量评估。

## 背景

- `RagRouterService`（qwen-turbo）判断 `needRag` 与 `kbIds`
- 原 `chat_cache_log` / `recall_log` 分散，无法关联单次用户提问
- `chat_session` / `chat_message` 表预留未用

## 数据模型

表 `chat_query_log`（Flyway V3），字段：

| 字段 | 说明 |
|------|------|
| session_id | H5 会话 ID |
| query | 用户问题 |
| reply_preview | 回复前 500 字 |
| cache_hit | 是否命中答案缓存 |
| need_rag | 路由是否判定需要 RAG |
| used_rag | 实际是否使用检索结果 |
| rag_kb_ids | 知识库 ID 列表 JSON |
| route_reason / route_confidence | 路由原因与置信度 |
| recall_count / top_scores_json | 召回条数与分数 |
| quality_score / quality_reason | 异步质量评估（准确度） |
| latency_ms | 端到端延迟 |

## 服务

- [`ChatQueryLogService`](../myrag-rag-monitor/...) — 异步写日志 + Micrometer 指标
- [`RecallQualityEvaluator.evaluateChatQueryAsync`](../myrag-rag-monitor/...) — Chat 走 RAG 时异步评估并回写质量分
- [`ChatService`](../myrag-chat/...) — 每次 chat/stream 调用 `logQueryAsync`

## 管理端

- 侧边栏 **用户问题** → `/chat-logs`
- 监控仪表盘合并展示：提问数、RAG 使用率、召回成功率、缓存命中率、平均质量分

## 指标说明

| 指标 | 含义 |
|------|------|
| chatRagRate | 实际使用 RAG 的提问占比 |
| chatRecallRate | 路由需 RAG 且召回数 &gt; 0 的占比 |
| cacheHitRate | 答案缓存命中占比 |
| avgQualityScore | 已评估记录的平均质量分 |

## 实施状态

已完成。
