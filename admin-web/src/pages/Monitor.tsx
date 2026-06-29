import { useQuery } from '@tanstack/react-query'
import { Button, Card, Col, Row, Statistic, Table, Tag } from 'antd'
import { adminApi } from '../api/client'

export default function MonitorPage() {
  const { data: metrics, refetch: refetchMetrics } = useQuery({
    queryKey: ['metrics'],
    queryFn: () => adminApi.getMetrics().then(r => r.data.data),
    refetchInterval: 10000,
  })

  const { data: logs, refetch: refetchLogs } = useQuery({
    queryKey: ['recall-logs'],
    queryFn: () => adminApi.getRecallLogs().then(r => r.data.data),
  })

  const { data: cacheLogs, refetch: refetchCacheLogs } = useQuery({
    queryKey: ['cache-logs'],
    queryFn: () => adminApi.getCacheLogs().then(r => r.data.data),
  })

  const { data: alerts } = useQuery({
    queryKey: ['alerts'],
    queryFn: () => adminApi.getAlerts().then(r => r.data.data),
  })

  const handleClearCache = async () => {
    await adminApi.clearCache()
    refetchMetrics()
    refetchCacheLogs()
  }

  const logColumns = [
    { title: '知识库', dataIndex: 'kbId', key: 'kbId', width: 120 },
    { title: '查询', dataIndex: 'query', key: 'query', ellipsis: true },
    { title: '结果数', dataIndex: 'resultCount', key: 'resultCount', width: 80 },
    { title: '延迟(ms)', dataIndex: 'latencyMs', key: 'latencyMs', width: 100 },
    { title: '时间', dataIndex: 'createdAt', key: 'createdAt', width: 180 },
  ]

  const cacheLogColumns = [
    { title: '查询', dataIndex: 'query', key: 'query', ellipsis: true },
    {
      title: '命中',
      dataIndex: 'hit',
      key: 'hit',
      width: 80,
      render: (hit: boolean) => (
        <Tag color={hit ? 'green' : 'default'}>{hit ? '命中' : '未命中'}</Tag>
      ),
    },
    {
      title: 'RAG',
      dataIndex: 'usedRag',
      key: 'usedRag',
      width: 70,
      render: (v: boolean) => (v ? '是' : '否'),
    },
    { title: '延迟(ms)', dataIndex: 'latencyMs', key: 'latencyMs', width: 100 },
    { title: '时间', dataIndex: 'createdAt', key: 'createdAt', width: 180 },
  ]

  const alertColumns = [
    { title: '知识库', dataIndex: 'kbId', key: 'kbId' },
    { title: '查询', dataIndex: 'query', key: 'query', ellipsis: true },
    {
      title: '质量分',
      dataIndex: 'qualityScore',
      key: 'qualityScore',
      render: (s: number) => <Tag color={s < 0.3 ? 'red' : 'orange'}>{s.toFixed(2)}</Tag>,
    },
    { title: '原因', dataIndex: 'reason', key: 'reason', ellipsis: true },
  ]

  return (
    <div>
      <h2>监控仪表盘</h2>
      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={6}>
          <Card><Statistic title="总召回次数" value={metrics?.totalRecalls ?? 0} /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title="空结果率" value={((metrics?.emptyRate ?? 0) * 100).toFixed(1)} suffix="%" /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title="平均召回延迟" value={(metrics?.avgLatencyMs ?? 0).toFixed(1)} suffix="ms" /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title="缓存命中率" value={((metrics?.cacheHitRate ?? 0) * 100).toFixed(1)} suffix="%" /></Card>
        </Col>
      </Row>

      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={6}>
          <Card><Statistic title="用户提问数" value={metrics?.totalChatQueries ?? 0} /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title="RAG 使用率" value={((metrics?.chatRagRate ?? 0) * 100).toFixed(1)} suffix="%" /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title="召回成功率" value={((metrics?.chatRecallRate ?? 0) * 100).toFixed(1)} suffix="%" /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title="平均质量分" value={(metrics?.avgQualityScore ?? 0).toFixed(2)} /></Card>
        </Col>
      </Row>

      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={8}>
          <Card><Statistic title="缓存命中次数" value={metrics?.totalCacheHits ?? 0} /></Card>
        </Col>
        <Col span={8}>
          <Card><Statistic title="缓存未命中" value={metrics?.totalCacheMisses ?? 0} /></Card>
        </Col>
        <Col span={8}>
          <Card>
            <Button danger onClick={handleClearCache}>清空答案缓存</Button>
          </Card>
        </Col>
      </Row>

      <h3>质量告警</h3>
      <Table rowKey="id" size="small" columns={alertColumns} dataSource={alerts?.data ?? alerts}
        style={{ marginBottom: 24 }} pagination={false} />

      <h3>缓存访问日志</h3>
      <Table rowKey="id" size="small" columns={cacheLogColumns}
        dataSource={cacheLogs?.content ?? []} pagination={{ pageSize: 10 }}
        style={{ marginBottom: 24 }} />

      <h3>召回日志</h3>
      <Table rowKey="id" size="small" columns={logColumns}
        dataSource={logs?.content ?? []} pagination={{ pageSize: 10 }} />
    </div>
  )
}
