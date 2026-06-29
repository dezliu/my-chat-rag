import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Input, Button, message, Alert } from 'antd'
import { useState, useEffect } from 'react'
import { adminApi } from '../api/client'

export default function SystemConfigPage() {
  const [prompt, setPrompt] = useState('')
  const queryClient = useQueryClient()

  const { data } = useQuery({
    queryKey: ['system-prompt'],
    queryFn: () => adminApi.getSystemPrompt().then(r => r.data.data),
  })

  useEffect(() => {
    if (data?.prompt) setPrompt(data.prompt)
  }, [data])

  const saveMutation = useMutation({
    mutationFn: () => adminApi.updateSystemPrompt(prompt),
    onSuccess: () => {
      message.success('保存成功')
      queryClient.invalidateQueries({ queryKey: ['system-prompt'] })
    },
    onError: () => message.error('保存失败'),
  })

  return (
    <div>
      <h2>系统配置</h2>
      <Alert
        type="warning"
        showIcon
        message="System Prompt 由服务端管理"
        description="此配置仅管理员可修改，Chat API 不接受客户端传入的 system 字段，防止用户覆盖系统背景。"
        style={{ marginBottom: 16 }}
      />
      <Input.TextArea
        rows={12}
        value={prompt}
        onChange={e => setPrompt(e.target.value)}
        placeholder="系统背景 Prompt..."
      />
      <Button type="primary" style={{ marginTop: 16 }} loading={saveMutation.isPending}
        onClick={() => saveMutation.mutate()}>
        保存
      </Button>
    </div>
  )
}
