import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Input, Button, message, Alert, Select, Tag, Divider, InputNumber } from 'antd'
import { useState, useEffect, useMemo } from 'react'
import { adminApi } from '../api/client'

type AiProvider = 'dashscope' | 'zhipuai'

const PROVIDER_OPTIONS = [
  { value: 'dashscope', label: '千问 (DashScope)' },
  { value: 'zhipuai', label: '智谱 (ZhipuAI)' },
]

const DASHSCOPE_CHAT_PRESETS = [
  { value: 'qwen-turbo', label: 'qwen-turbo（轻量）' },
  { value: 'qwen-plus', label: 'qwen-plus（默认）' },
  { value: 'qwen-max', label: 'qwen-max（高质量）' },
  { value: 'qwen-long', label: 'qwen-long（长文本）' },
]

const ZHIPU_CHAT_PRESETS = [
  { value: 'glm-4-flash', label: 'glm-4-flash（路由推荐）' },
  { value: 'glm-4-air', label: 'glm-4-air（轻量）' },
  { value: 'glm-4-plus', label: 'glm-4-plus（默认）' },
  { value: 'glm-4.6', label: 'glm-4.6（高质量）' },
]

const DASHSCOPE_EMBEDDING_MODELS = [
  { value: 'text-embedding-v3', label: 'text-embedding-v3（1024 维）' },
  { value: 'text-embedding-v2', label: 'text-embedding-v2' },
]

const ZHIPU_EMBEDDING_MODELS = [
  { value: 'embedding-3', label: 'embedding-3（1536 维）' },
]

const PROVIDER_DEFAULTS: Record<AiProvider, {
  routerModel: string
  chatModel: string
  embeddingModel: string
  embeddingDimensions: number
  baseUrl: string
}> = {
  dashscope: {
    routerModel: 'qwen-turbo',
    chatModel: 'qwen-plus',
    embeddingModel: 'text-embedding-v3',
    embeddingDimensions: 1024,
    baseUrl: '',
  },
  zhipuai: {
    routerModel: 'glm-4-flash',
    chatModel: 'glm-4-plus',
    embeddingModel: 'embedding-3',
    embeddingDimensions: 1536,
    baseUrl: 'https://open.bigmodel.cn/api/paas',
  },
}

function getChatPresets(provider: AiProvider) {
  return provider === 'zhipuai' ? ZHIPU_CHAT_PRESETS : DASHSCOPE_CHAT_PRESETS
}

function getEmbeddingModels(provider: AiProvider) {
  return provider === 'zhipuai' ? ZHIPU_EMBEDDING_MODELS : DASHSCOPE_EMBEDDING_MODELS
}

function buildModelOptions(
  presets: { value: string; label: string }[],
  customModels: string[],
  currentModel: string,
) {
  const presetValues = new Set(presets.map(m => m.value))
  const options = [...presets]
  const seen = new Set(presetValues)
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
  presets: { value: string; label: string }[]
}

function CustomModelField({
  label,
  model,
  onModelChange,
  customModels,
  onCustomModelsChange,
  defaultModel,
  inputPlaceholder,
  presets,
}: CustomModelFieldProps) {
  const [newModel, setNewModel] = useState('')
  const presetValues = useMemo(() => new Set(presets.map(m => m.value)), [presets])
  const options = useMemo(
    () => buildModelOptions(presets, customModels, model),
    [presets, customModels, model],
  )

  const addCustomModel = () => {
    const value = newModel.trim()
    if (!value) {
      message.warning('请输入模型名称')
      return
    }
    if (presetValues.has(value) || customModels.includes(value)) {
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
  const [provider, setProvider] = useState<AiProvider>('dashscope')
  const [apiKey, setApiKey] = useState('')
  const [baseUrl, setBaseUrl] = useState('')
  const [routerModel, setRouterModel] = useState('qwen-turbo')
  const [chatModel, setChatModel] = useState('qwen-plus')
  const [embeddingModel, setEmbeddingModel] = useState('text-embedding-v3')
  const [embeddingDimensions, setEmbeddingDimensions] = useState(1024)
  const [customRouterModels, setCustomRouterModels] = useState<string[]>([])
  const [customChatModels, setCustomChatModels] = useState<string[]>([])
  const [initialProvider, setInitialProvider] = useState<AiProvider>('dashscope')
  const [initialEmbeddingModel, setInitialEmbeddingModel] = useState('text-embedding-v3')
  const [initialEmbeddingDimensions, setInitialEmbeddingDimensions] = useState(1024)
  const queryClient = useQueryClient()

  const chatPresets = useMemo(() => getChatPresets(provider), [provider])
  const embeddingModels = useMemo(() => getEmbeddingModels(provider), [provider])
  const providerDefaults = PROVIDER_DEFAULTS[provider]

  const embeddingChanged = embeddingModel !== initialEmbeddingModel
    || embeddingDimensions !== initialEmbeddingDimensions
    || provider !== initialProvider

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
      const loadedProvider = (aiConfig.provider ?? 'dashscope') as AiProvider
      setProvider(loadedProvider)
      setInitialProvider(loadedProvider)
      setBaseUrl(aiConfig.baseUrl ?? PROVIDER_DEFAULTS[loadedProvider].baseUrl)
      setRouterModel(aiConfig.routerModel)
      setChatModel(aiConfig.chatModel)
      setEmbeddingModel(aiConfig.embeddingModel)
      setInitialEmbeddingModel(aiConfig.embeddingModel)
      setEmbeddingDimensions(aiConfig.embeddingDimensions ?? PROVIDER_DEFAULTS[loadedProvider].embeddingDimensions)
      setInitialEmbeddingDimensions(aiConfig.embeddingDimensions ?? PROVIDER_DEFAULTS[loadedProvider].embeddingDimensions)
      setCustomRouterModels(aiConfig.customRouterModels ?? [])
      setCustomChatModels(aiConfig.customChatModels ?? [])
      setApiKey('')
    }
  }, [aiConfig])

  const handleProviderChange = (next: AiProvider) => {
    const defaults = PROVIDER_DEFAULTS[next]
    setProvider(next)
    setRouterModel(defaults.routerModel)
    setChatModel(defaults.chatModel)
    setEmbeddingModel(defaults.embeddingModel)
    setEmbeddingDimensions(defaults.embeddingDimensions)
    setBaseUrl(defaults.baseUrl)
    setCustomRouterModels([])
    setCustomChatModels([])
  }

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
      provider,
      baseUrl: provider === 'zhipuai' ? baseUrl : undefined,
      embeddingDimensions,
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
      setInitialProvider(provider)
      setInitialEmbeddingModel(embeddingModel)
      setInitialEmbeddingDimensions(embeddingDimensions)
      queryClient.invalidateQueries({ queryKey: ['ai-config'] })
    },
    onError: () => message.error('AI 配置保存失败'),
  })

  const apiKeyLabel = provider === 'zhipuai' ? '智谱 API Key' : 'DashScope API Key'
  const apiKeyEnvHint = provider === 'zhipuai' ? 'AI_ZHIPUAI_API_KEY' : 'AI_DASHSCOPE_API_KEY'

  return (
    <div>
      <h2>系统配置</h2>

      <h3 style={{ marginTop: 24 }}>AI 配置</h3>
      <Alert
        type="warning"
        showIcon
        message="API Key 安全提示"
        description={`Key 可存入数据库并在界面脱敏展示；生产环境仍建议通过环境变量 ${apiKeyEnvHint} 注入。留空 API Key 输入框表示不修改已有 Key。`}
        style={{ marginBottom: 16 }}
      />
      {embeddingChanged && (
        <Alert
          type="error"
          showIcon
          message="Embedding 配置已变更"
          description="已有知识库的向量是按旧厂商/维度生成的。切换 Embedding 后需对各知识库文档重新入库，否则检索会失败。"
          style={{ marginBottom: 16 }}
        />
      )}
      <div style={{ marginBottom: 16 }}>
        <div style={{ marginBottom: 8 }}>AI 厂商</div>
        <Select
          value={provider}
          onChange={handleProviderChange}
          options={PROVIDER_OPTIONS}
          style={{ width: 280 }}
        />
      </div>
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
        <div style={{ marginBottom: 8 }}>{apiKeyLabel}</div>
        <Input.Password
          value={apiKey}
          onChange={e => setApiKey(e.target.value)}
          placeholder={aiConfig?.apiKeyConfigured ? `已配置 ${aiConfig.apiKeyMasked}，留空不修改` : '输入 API Key'}
          style={{ maxWidth: 480 }}
        />
      </div>
      {provider === 'zhipuai' && (
        <div style={{ marginBottom: 16 }}>
          <div style={{ marginBottom: 8 }}>智谱 Base URL（可选）</div>
          <Input
            value={baseUrl}
            onChange={e => setBaseUrl(e.target.value)}
            placeholder="https://open.bigmodel.cn/api/paas"
            style={{ maxWidth: 480 }}
          />
        </div>
      )}
      <div style={{ display: 'flex', gap: 16, flexWrap: 'wrap', marginBottom: 16 }}>
        <CustomModelField
          label="路由模型"
          model={routerModel}
          onModelChange={setRouterModel}
          customModels={customRouterModels}
          onCustomModelsChange={setCustomRouterModels}
          defaultModel={providerDefaults.routerModel}
          inputPlaceholder="输入自定义路由模型名"
          presets={chatPresets}
        />
        <CustomModelField
          label="对话模型"
          model={chatModel}
          onModelChange={setChatModel}
          customModels={customChatModels}
          onCustomModelsChange={setCustomChatModels}
          defaultModel={providerDefaults.chatModel}
          inputPlaceholder="输入自定义对话模型名"
          presets={chatPresets}
        />
        <div>
          <div style={{ marginBottom: 8 }}>Embedding 模型</div>
          <Select
            value={embeddingModel}
            onChange={setEmbeddingModel}
            options={embeddingModels}
            style={{ width: 240 }}
            showSearch
          />
        </div>
        <div>
          <div style={{ marginBottom: 8 }}>Embedding 维度</div>
          <InputNumber
            min={128}
            max={4096}
            value={embeddingDimensions}
            onChange={v => setEmbeddingDimensions(v ?? providerDefaults.embeddingDimensions)}
            style={{ width: 160 }}
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
