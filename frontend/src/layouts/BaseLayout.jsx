import { Layout, Menu, Avatar, Dropdown, Alert, Space } from 'antd'
import { UserOutlined, LogoutOutlined, KeyOutlined } from '@ant-design/icons'
import { Outlet, useNavigate, useLocation } from 'react-router-dom'
import { useAuthStore } from '../store.js'

const { Header, Sider, Content } = Layout

export default function BaseLayout({ menuItems, basePath, title }) {
  const navigate = useNavigate()
  const location = useLocation()
  const { user, logout } = useAuthStore()

  const handleLogout = () => { logout(); navigate('/login') }

  const userMenu = {
    items: [
      { key: 'pwd', icon: <KeyOutlined />, label: '修改密码', onClick: () => navigate(`${basePath}/password`) },
      { type: 'divider' },
      { key: 'logout', icon: <LogoutOutlined />, label: '退出登录', onClick: handleLogout }
    ]
  }

  const current = location.pathname.endsWith(basePath) || location.pathname === basePath
    ? basePath
    : location.pathname

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider width={220} theme="light" style={{ borderRight: '1px solid #f0f0f0' }}>
        <div style={{ padding: '16px 16px', borderBottom: '1px solid #f0f0f0' }}>
          <div style={{ color: '#1f4d8c', fontWeight: 700, fontSize: 14, lineHeight: '1.5' }}>浙江工商大学</div>
          <div style={{ color: '#1f4d8c', fontWeight: 600, fontSize: 13, lineHeight: '1.5' }}>本科生奖学金评选系统</div>
        </div>
        <Menu
          mode="inline"
          selectedKeys={[current]}
          items={menuItems.map((m) => ({ ...m, onClick: () => navigate(m.key) }))}
          style={{ borderInlineEnd: 'none' }}
        />
      </Sider>
      <Layout>
        <Header style={{ background: '#fff', display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '0 24px', borderBottom: '1px solid #f0f0f0' }}>
          <div style={{ fontSize: 16, color: '#262626' }}>{title}</div>
          <Dropdown menu={userMenu}>
            <Space style={{ cursor: 'pointer' }}>
              <Avatar icon={<UserOutlined />} />
              <span>{user?.name}（{user?.account}）</span>
            </Space>
          </Dropdown>
        </Header>
        {user?.usingInitialPassword && (
          <Alert
            type="warning"
            banner
            showIcon
            message="您当前正在使用初始密码，建议前往「修改密码」更换为自定义密码。"
          />
        )}
        <Content style={{ padding: 24 }}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  )
}
