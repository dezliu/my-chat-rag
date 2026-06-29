import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Input, Button, message, Alert, Select, Tag, Divider } from 'antd'
import { useState, useEffect, useMemo } from 'react'
import { adminApi } from '../api/client'

const PRESET_MODELS = [
  { value: 'qwen-turbo', label: 'qwen-turbo（轻量）' },
  { value: 'qwen-plus', label: 'qwen-plus（默认）' },
  { value: 'qwen-max', label: 'qwen-max（高质量）' },
  { value: 'qwen-long', label: 'qwen-long（长文本）' },
]

const PRESET_MODEL_VALUES = new Set(PRESET_MODELS.map(m => m.value))

const EMBEDDING_MODELS = [
  { value: 'text-embedding-v3', label: 'text-embedding-v3' },
  { value: 'text-embedding-v2', label: 'text-embedding-v2' },
]

function buildModelOptions(
  customModels: string[],
  currentModel: string,
) {
  const options = [...PRESET_MODELS]
  const seen = new Set(PRESET_MODEL_VALUES)
  for (const model of customModels) {
    if (!seen.has(model)) {
      options.push({ value: model, label: `${model}（自定义）` })
      seen.add(model)
    }
  }
  if (currentModel && !seen.has(currentModel)) {
    options.push({ value: currentModel, label: `${currentModel}（当前）` })
  }
  return options
}

interface CustomModelFieldProps {
  label: string
  model: string
  onModelChange: (value: string) => void
  customModels: string[]
  onCustomModelsChange: (models: string[]) => void
  defaultModel: string
  inputPlaceholder: string
}

function CustomModelField({
  label,
  model,
  onModelChange,
  customModels,
  onCustomModelsChange,
  defaultModel,
  inputPlaceholder,
}: CustomModelFieldProps) {
  const [newModel, setNewModel] = useState('')
  const options = useMemo(
    () => buildModelOptions(customModels, model),
    [customModels, model],
  )

  const addCustomModel = () => {
    const value = newModel.trim()
    if (!value) {
      message.warning('请输入模型名称')
      return
    }
    if (PRESET_MODEL_VALUES.has(value) || customModels.includes(value)) {
      message.warning('该模型已在列表中')
      onModelChange(value)
      setNewModel('')
      return
    }
    onCustomModelsChange([...customModels, value])
    onModelChange(value)
    setNewModel('')
    message.success(`已添加自定义模型：${value}`)
  }

  const removeCustomModel = (value: string) => {
    onCustomModelsChange(customModels.filter(m => m !== value))
    if (model === value) {
      onModelChange(defaultModel)
    }
  }

  return (
    <div>
      <div style={{ marginBottom: 8 }}>{label}</div>
      <Select
        value={model}
        onChange={onModelChange}
        options={options}
        style={{ width: 280 }}
        showSearch
        optionFilterProp="label"
      />
      <div style={{ marginTop: 8, display: 'flex', gap: 8, maxWidth: 360 }}>
        <Input
          value={newModel}
          onChange={e => setNewModel(e.target.value)}
          placeholder={inputPlaceholder}
          onPressEnter={addCustomModel}
        />
        <Button onClick={addCustomModel}>添加</Button>
      </div>
      {customModels.length > 0 && (
        <div style={{ marginTop: 8, maxWidth: 360 }}>
          {customModels.map(value => (
            <Tag
              key={value}
              closable
              onClose={() => removeCustomModel(value)}
              style={{ marginBottom: 4 }}
            >
              {value}
            </Tag>
          ))}
        </div>
      )}
    </div>
  )
}

export default function SystemConfigPage() {
  const [prompt, setPrompt] = useState('')
  const [apiKey, setApiKey] = useState('')
  const [routerModel, setRouterModel] = useState('qwen-turbo')
  const [chatModel, setChatModel] = useState('qwen-plus')
  const [embeddingModel, setEmbeddingModel] = useState('text-embedding-v3')
  const [customRouterModels, setCustomRouterModels] = useState<string[]>([])
  const [customChatModels, setCustomChatModels] = useState<string[]>([])
  const queryClient = useQueryClient()

  const { data: promptData } = useQuery({
    queryKey: ['system-prompt'],
    queryFn: () => adminApi.getSystemPrompt().then(r => r.data.data),
  })

  const { data: aiConfig } = useQuery({
    queryKey: ['ai-config'],
    queryFn: () => adminApi.getAiConfig().then(r => r.data.data),
  })

  useEffect(() => {
    if (promptData?.prompt) setPrompt(promptData.prompt)
  }, [promptData])

  useEffect(() => {
    if (aiConfig) {
      setRouterModel(aiConfig.routerModel)
      setChatModel(aiConfig.chatModel)
      setEmbeddingModel(aiConfig.embeddingModel)
      setCustomRouterModels(aiConfig.customRouterModels ?? [])
      setCustomChatModels(aiConfig.customChatModels ?? [])
      setApiKey('')
    }
  }, [aiConfig])

  const savePromptMutation = useMutation({
    mutationFn: () => adminApi.updateSystemPrompt(prompt),
    onSuccess: () => {
      message.success('System Prompt 保存成功')
      queryClient.invalidateQueries({ queryKey: ['system-prompt'] })
    },
    onError: () => message.error('保存失败'),
  })

  const saveAiMutation = useMutation({
    mutationFn: () => adminApi.updateAiConfig({
      apiKey: apiKey.trim() || undefined,
      routerModel,
      chatModel,
      embeddingModel,
      customRouterModels,
      customChatModels,
    }),
    onSuccess: () => {
      message.success('AI 配置保存成功，已即时生效')
      setApiKey('')
      queryClient.invalidateQueries({ queryKey: ['ai-config'] })
    },
    onError: () => message.error('AI 配置保存失败'),
  })

  return (
    <div>
      <h2>系统配置</h2>

      <h3 style={{ marginTop: 24 }}>AI 配置</h3>
      <Alert
        type="warning"
        showIcon
        message="API Key 安全提示"
        description="Key 可存入数据库并在界面脱敏展示；生产环境仍建议通过环境变量 AI_DASHSCOPE_API_KEY 注入。留空 API Key 输入框表示不修改已有 Key。"
        style={{ marginBottom: 16 }}
      />
      <div style={{ marginBottom: 12 }}>
        <span style={{ marginRight: 8 }}>当前 Key 来源：</span>
        {aiConfig?.apiKeySource === 'db' ? (
          <Tag color="blue">数据库</Tag>
        ) : (
          <Tag color="green">环境变量</Tag>
        )}
        {aiConfig?.apiKeyConfigured && (
          <span style={{ marginLeft: 12, color: '#666' }}>已配置：{aiConfig.apiKeyMasked}</span>
        )}
      </div>
      <div style={{ marginBottom: 16 }}>
        <div style={{ marginBottom: 8 }}>DashScope API Key</div>
        <Input.Password
          value={apiKey}
          onChange={e => setApiKey(e.target.value)}
          placeholder={aiConfig?.apiKeyConfigured ? `已配置 ${aiConfig.apiKeyMasked}，留空不修改` : '输入 API Key'}
          style={{ maxWidth: 480 }}
        />
      </div>
      <div style={{ display: 'flex', gap: 16, flexWrap: 'wrap', marginBottom: 16 }}>
        <CustomModelField
          label="路由模型"
          model={routerModel}
          onModelChange={setRouterModel}
          customModels={customRouterModels}
          onCustomModelsChange={setCustomRouterModels}
          defaultModel="qwen-turbo"
          inputPlaceholder="输入自定义路由模型名"
        />
        <CustomModelField
          label="对话模型"
          model={chatModel}
          onModelChange={setChatModel}
          customModels={customChatModels}
          onCustomModelsChange={setCustomChatModels}
          defaultModel="qwen-plus"
          inputPlaceholder="输入自定义对话模型名"
        />
        <div>
          <div style={{ marginBottom: 8 }}>Embedding 模型</div>
          <Select
            value={embeddingModel}
            onChange={setEmbeddingModel}
            options={EMBEDDING_MODELS}
            style={{ width: 240 }}
            showSearch
          />
        </div>
      </div>
      <Button type="primary" loading={saveAiMutation.isPending} onClick={() => saveAiMutation.mutate()}>
        保存 AI 配置
      </Button>

      <Divider />

      <h3>System Prompt</h3>
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
      <Button type="primary" style={{ marginTop: 16 }} loading={savePromptMutation.isPending}
        onClick={() => savePromptMutation.mutate()}>
        保存 System Prompt
      </Button>
    </div>
  )
}
