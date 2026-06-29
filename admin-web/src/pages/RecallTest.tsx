import { useState } from 'react'
import { useQuery, useMutation } from '@tanstack/react-query'
import { Select, Input, Button, Card, List, Tag, Spin, Statistic, Row, Col } from 'antd'
import { adminApi } from '../api/client'

export default function RecallTestPage() {
  const [kbId, setKbId] = useState<string>()
  const [query, setQuery] = useState('')

  const { data: kbs } = useQuery({
    queryKey: ['knowledge-bases'],
    queryFn: () => adminApi.listKnowledgeBases().then(r => r.data.data),
  })

  const searchMutation = useMutation({
    mutationFn: () => adminApi.recallTest({ kbIds: [kbId!], query, topK: 5 }),
  })

  const result = searchMutation.data?.data.data

  return (
    <div>
      <h2>召回测试</h2>
      <div style={{ marginBottom: 16, display: 'flex', gap: 12 }}>
        <Select
          style={{ width: 300 }}
          placeholder="选择知识库"
          value={kbId}
          onChange={setKbId}
          options={kbs?.map(kb => ({ label: kb.name, value: kb.id }))}
        />
        <Input
          style={{ flex: 1 }}
          placeholder="输入测试查询..."
          value={query}
          onChange={e => setQuery(e.target.value)}
          onPressEnter={() => kbId && query && searchMutation.mutate()}
        />
        <Button type="primary" disabled={!kbId || !query} loading={searchMutation.isPending}
          onClick={() => searchMutation.mutate()}>
          检索
        </Button>
      </div>

      {searchMutation.isPending && <Spin />}
      {result && (
        <>
          <Row gutter={16} style={{ marginBottom: 16 }}>
            <Col><Statistic title="结果数" value={result.results.length} /></Col>
            <Col><Statistic title="延迟 (ms)" value={result.latencyMs} /></Col>
          </Row>
          <List
            dataSource={result.results}
            renderItem={(item, idx) => (
              <Card key={idx} size="small" style={{ marginBottom: 8 }}
                title={<><Tag color="blue">#{idx + 1}</Tag> score: {item.score.toFixed(4)}</>}>
                <p style={{ whiteSpace: 'pre-wrap' }}>{item.content}</p>
              </Card>
            )}
          />
        </>
      )}
    </div>
  )
}
