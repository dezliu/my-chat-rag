import { useQuery } from '@tanstack/react-query'
import { Card, Col, Row, Statistic, Table, Tag } from 'antd'
import { adminApi } from '../api/client'

export default function MonitorPage() {
  const { data: metrics } = useQuery({
    queryKey: ['metrics'],
    queryFn: () => adminApi.getMetrics().then(r => r.data.data),
    refetchInterval: 10000,
  })

  const { data: logs } = useQuery({
    queryKey: ['recall-logs'],
    queryFn: () => adminApi.getRecallLogs().then(r => r.data.data),
  })

  const { data: alerts } = useQuery({
    queryKey: ['alerts'],
    queryFn: () => adminApi.getAlerts().then(r => r.data.data),
  })

  const logColumns = [
    { title: '知识库', dataIndex: 'kbId', key: 'kbId', width: 120 },
    { title: '查询', dataIndex: 'query', key: 'query', ellipsis: true },
    { title: '结果数', dataIndex: 'resultCount', key: 'resultCount', width: 80 },
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
        <Col span={8}>
          <Card><Statistic title="总召回次数" value={metrics?.totalRecalls ?? 0} /></Card>
        </Col>
        <Col span={8}>
          <Card><Statistic title="空结果率" value={((metrics?.emptyRate ?? 0) * 100).toFixed(1)} suffix="%" /></Card>
        </Col>
        <Col span={8}>
          <Card><Statistic title="平均延迟" value={(metrics?.avgLatencyMs ?? 0).toFixed(1)} suffix="ms" /></Card>
        </Col>
      </Row>

      <h3>质量告警</h3>
      <Table rowKey="id" size="small" columns={alertColumns} dataSource={alerts?.data ?? alerts}
        style={{ marginBottom: 24 }} pagination={false} />

      <h3>召回日志</h3>
      <Table rowKey="id" size="small" columns={logColumns}
        dataSource={logs?.content ?? []} pagination={{ pageSize: 10 }} />
    </div>
  )
}
