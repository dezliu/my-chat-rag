import { useQuery } from '@tanstack/react-query'
import { Card, Col, Row, Statistic, Table, Tag } from 'antd'
import { adminApi } from '../api/client'

interface ChatQueryLog {
  id: string
  sessionId: string
  query: string
  replyPreview: string
  cacheHit: boolean
  needRag: boolean
  usedRag: boolean
  ragKbIds: string
  routeReason: string
  routeConfidence: number
  recallCount: number
  qualityScore: number | null
  qualityReason: string
  latencyMs: number
  createdAt: string
}

export default function ChatLogsPage() {
  const { data: metrics } = useQuery({
    queryKey: ['metrics'],
    queryFn: () => adminApi.getMetrics().then(r => r.data.data),
    refetchInterval: 10000,
  })

  const { data: logs } = useQuery({
    queryKey: ['chat-logs'],
    queryFn: () => adminApi.getChatLogs().then(r => r.data.data),
  })

  const columns = [
    { title: '时间', dataIndex: 'createdAt', key: 'createdAt', width: 170 },
    { title: 'Session', dataIndex: 'sessionId', key: 'sessionId', width: 120, ellipsis: true },
    { title: '用户问题', dataIndex: 'query', key: 'query', ellipsis: true },
    {
      title: '缓存',
      dataIndex: 'cacheHit',
      key: 'cacheHit',
      width: 70,
      render: (v: boolean) => <Tag color={v ? 'green' : 'default'}>{v ? '命中' : '未命中'}</Tag>,
    },
    {
      title: '路由RAG',
      dataIndex: 'needRag',
      key: 'needRag',
      width: 80,
      render: (v: boolean) => (v ? '是' : '否'),
    },
    {
      title: '实际RAG',
      dataIndex: 'usedRag',
      key: 'usedRag',
      width: 80,
      render: (v: boolean) => (v ? '是' : '否'),
    },
    { title: '召回数', dataIndex: 'recallCount', key: 'recallCount', width: 70 },
    {
      title: '质量分',
      dataIndex: 'qualityScore',
      key: 'qualityScore',
      width: 80,
      render: (s: number | null) => (s != null ? s.toFixed(2) : '-'),
    },
    { title: '延迟(ms)', dataIndex: 'latencyMs', key: 'latencyMs', width: 90 },
    { title: '路由原因', dataIndex: 'routeReason', key: 'routeReason', ellipsis: true },
  ]

  return (
    <div>
      <h2>用户问题日志</h2>
      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={6}>
          <Card><Statistic title="总提问次数" value={metrics?.totalChatQueries ?? 0} /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title="RAG 使用率" value={((metrics?.chatRagRate ?? 0) * 100).toFixed(1)} suffix="%" /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title="召回成功率" value={((metrics?.chatRecallRate ?? 0) * 100).toFixed(1)} suffix="%" /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title="缓存命中率" value={((metrics?.cacheHitRate ?? 0) * 100).toFixed(1)} suffix="%" /></Card>
        </Col>
      </Row>
      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={6}>
          <Card><Statistic title="平均质量分" value={(metrics?.avgQualityScore ?? 0).toFixed(2)} /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title="缓存命中" value={metrics?.totalCacheHits ?? 0} /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title="缓存未命中" value={metrics?.totalCacheMisses ?? 0} /></Card>
        </Col>
      </Row>

      <Table
        rowKey="id"
        size="small"
        columns={columns}
        dataSource={(logs?.content ?? []) as ChatQueryLog[]}
        pagination={{ pageSize: 15 }}
        expandable={{
          expandedRowRender: (record: ChatQueryLog) => (
            <div style={{ whiteSpace: 'pre-wrap' }}>
              <p><strong>回复预览：</strong>{record.replyPreview || '-'}</p>
              <p><strong>知识库：</strong>{record.ragKbIds || '[]'}</p>
              {record.qualityReason && <p><strong>质量评估：</strong>{record.qualityReason}</p>}
            </div>
          ),
        }}
      />
    </div>
  )
}
