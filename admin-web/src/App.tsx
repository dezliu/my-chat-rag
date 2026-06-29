import { BrowserRouter, Routes, Route, Link, useLocation } from 'react-router-dom'
import { Layout, Menu } from 'antd'
import {
  DatabaseOutlined,
  FileOutlined,
  SearchOutlined,
  DashboardOutlined,
  SettingOutlined,
} from '@ant-design/icons'
import KnowledgeBasePage from './pages/KnowledgeBase'
import DocumentsPage from './pages/Documents'
import RecallTestPage from './pages/RecallTest'
import MonitorPage from './pages/Monitor'
import SystemConfigPage from './pages/SystemConfig'

const { Header, Sider, Content } = Layout

function AppLayout() {
  const location = useLocation()
  const selectedKey = location.pathname.split('/')[1] || 'knowledge-bases'

  const menuItems = [
    { key: 'knowledge-bases', icon: <DatabaseOutlined />, label: <Link to="/knowledge-bases">知识库管理</Link> },
    { key: 'documents', icon: <FileOutlined />, label: <Link to="/documents">文档管理</Link> },
    { key: 'recall-test', icon: <SearchOutlined />, label: <Link to="/recall-test">召回测试</Link> },
    { key: 'monitor', icon: <DashboardOutlined />, label: <Link to="/monitor">监控仪表盘</Link> },
    { key: 'system-config', icon: <SettingOutlined />, label: <Link to="/system-config">系统配置</Link> },
  ]

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider theme="dark" width={220}>
        <div style={{ color: '#fff', padding: '16px 24px', fontSize: 18, fontWeight: 600 }}>
          MyRAG Admin
        </div>
        <Menu theme="dark" mode="inline" selectedKeys={[selectedKey]} items={menuItems} />
      </Sider>
      <Layout>
        <Header style={{ background: '#fff', padding: '0 24px', fontSize: 16, fontWeight: 500 }}>
          AI Chat + RAG 管理后台
        </Header>
        <Content style={{ margin: 24, padding: 24, background: '#fff', borderRadius: 8 }}>
          <Routes>
            <Route path="/" element={<KnowledgeBasePage />} />
            <Route path="/knowledge-bases" element={<KnowledgeBasePage />} />
            <Route path="/documents" element={<DocumentsPage />} />
            <Route path="/recall-test" element={<RecallTestPage />} />
            <Route path="/monitor" element={<MonitorPage />} />
            <Route path="/system-config" element={<SystemConfigPage />} />
          </Routes>
        </Content>
      </Layout>
    </Layout>
  )
}

export default function App() {
  return (
    <BrowserRouter>
      <AppLayout />
    </BrowserRouter>
  )
}
