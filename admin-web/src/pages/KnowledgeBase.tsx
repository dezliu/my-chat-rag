import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Table, Button, Modal, Form, Input, message, Popconfirm, Tag } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import { adminApi, KnowledgeBase } from '../api/client'

export default function KnowledgeBasePage() {
  const [open, setOpen] = useState(false)
  const [form] = Form.useForm()
  const queryClient = useQueryClient()

  const { data, isLoading } = useQuery({
    queryKey: ['knowledge-bases'],
    queryFn: () => adminApi.listKnowledgeBases().then(r => r.data.data),
  })

  const createMutation = useMutation({
    mutationFn: (values: { name: string; description?: string }) =>
      adminApi.createKnowledgeBase(values),
    onSuccess: () => {
      message.success('创建成功')
      setOpen(false)
      form.resetFields()
      queryClient.invalidateQueries({ queryKey: ['knowledge-bases'] })
    },
    onError: () => message.error('创建失败'),
  })

  const deleteMutation = useMutation({
    mutationFn: (id: string) => adminApi.deleteKnowledgeBase(id),
    onSuccess: () => {
      message.success('删除成功')
      queryClient.invalidateQueries({ queryKey: ['knowledge-bases'] })
    },
  })

  const columns = [
    { title: '名称', dataIndex: 'name', key: 'name' },
    { title: '描述', dataIndex: 'description', key: 'description', ellipsis: true },
    { title: 'Collection', dataIndex: 'collectionName', key: 'collectionName' },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => (
        <Tag color={status === 'ACTIVE' ? 'green' : 'red'}>{status}</Tag>
      ),
    },
    { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt' },
    {
      title: '操作',
      key: 'action',
      render: (_: unknown, record: KnowledgeBase) => (
        <Popconfirm title="确认删除？" onConfirm={() => deleteMutation.mutate(record.id)}>
          <Button danger size="small">删除</Button>
        </Popconfirm>
      ),
    },
  ]

  return (
    <div>
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between' }}>
        <h2 style={{ margin: 0 }}>知识库管理</h2>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setOpen(true)}>
          新建知识库
        </Button>
      </div>
      <Table rowKey="id" loading={isLoading} columns={columns} dataSource={data} />

      <Modal title="新建知识库" open={open} onCancel={() => setOpen(false)}
        onOk={() => form.validateFields().then(v => createMutation.mutate(v))}
        confirmLoading={createMutation.isPending}>
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="名称" rules={[{ required: true }]}>
            <Input placeholder="例如：产品文档" />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={3} placeholder="知识库用途描述，供路由小模型参考" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
