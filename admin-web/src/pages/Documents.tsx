import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Select, Upload, Table, Button, message, Popconfirm, Tag } from 'antd'
import { UploadOutlined } from '@ant-design/icons'
import { adminApi, Document } from '../api/client'

export default function DocumentsPage() {
  const [kbId, setKbId] = useState<string>()
  const queryClient = useQueryClient()

  const { data: kbs } = useQuery({
    queryKey: ['knowledge-bases'],
    queryFn: () => adminApi.listKnowledgeBases().then(r => r.data.data),
  })

  const { data: docs, isLoading } = useQuery({
    queryKey: ['documents', kbId],
    queryFn: () => adminApi.listDocuments(kbId!).then(r => r.data.data),
    enabled: !!kbId,
  })

  const deleteMutation = useMutation({
    mutationFn: (docId: string) => adminApi.deleteDocument(docId),
    onSuccess: () => {
      message.success('删除成功')
      queryClient.invalidateQueries({ queryKey: ['documents', kbId] })
    },
  })

  const columns = [
    { title: '文件名', dataIndex: 'filename', key: 'filename' },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (s: string) => {
        const color = s === 'INDEXED' ? 'green' : s === 'FAILED' ? 'red' : 'processing'
        return <Tag color={color}>{s}</Tag>
      },
    },
    { title: '分块数', dataIndex: 'chunkCount', key: 'chunkCount' },
    { title: '上传时间', dataIndex: 'createdAt', key: 'createdAt' },
    {
      title: '操作',
      key: 'action',
      render: (_: unknown, record: Document) => (
        <Popconfirm title="确认删除？" onConfirm={() => deleteMutation.mutate(record.id)}>
          <Button danger size="small">删除</Button>
        </Popconfirm>
      ),
    },
  ]

  return (
    <div>
      <h2>文档管理</h2>
      <div style={{ marginBottom: 16, display: 'flex', gap: 16, alignItems: 'center' }}>
        <Select
          style={{ width: 300 }}
          placeholder="选择知识库"
          value={kbId}
          onChange={setKbId}
          options={kbs?.map(kb => ({ label: kb.name, value: kb.id }))}
        />
        {kbId && (
          <Upload
            accept=".txt,.md,.pdf,.docx"
            showUploadList={false}
            customRequest={async ({ file, onSuccess, onError }) => {
              try {
                await adminApi.uploadDocument(kbId, file as File)
                message.success('上传并索引成功')
                queryClient.invalidateQueries({ queryKey: ['documents', kbId] })
                onSuccess?.(null)
              } catch {
                message.error('上传失败')
                onError?.(new Error('upload failed'))
              }
            }}
          >
            <Button icon={<UploadOutlined />}>上传文档</Button>
          </Upload>
        )}
      </div>
      <Table rowKey="id" loading={isLoading} columns={columns} dataSource={docs} />
    </div>
  )
}
