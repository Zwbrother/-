import { useState } from 'react'
import { Form, Input, Button, Alert, Tag, Space } from 'antd'
import { UserOutlined, LockOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import api from '../api.js'
import { useAuthStore } from '../store.js'

const samples = [
  { label: '系统管理员', account: 'admin', password: 'admin@2026' },
  { label: '辅导员 赵', account: 'T2023001', password: 'T2023001@zjsu' },
  { label: '辅导员 钱', account: 'T2023002', password: 'T2023002@zjsu' },
  { label: '学生 张明', account: '20231001', password: '123456' },
  { label: '学生 吴磊', account: '20231008', password: '123456' }
]

export default function Login() {
  const [form] = Form.useForm()
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()
  const setAuth = useAuthStore((s) => s.setAuth)

  const submit = async (values) => {
    setLoading(true)
    try {
      const data = await api.post('/auth/login', values)
      setAuth(data.token, {
        account: data.account, role: data.role, name: data.name,
        usingInitialPassword: data.usingInitialPassword
      })
      if (data.role === 'STUDENT') navigate('/student')
      else if (data.role === 'COUNSELOR') navigate('/counselor')
      else navigate('/admin')
    } finally { setLoading(false) }
  }

  const fill = (s) => form.setFieldsValue({ account: s.account, password: s.password })

  return (
    <div className="login-bg">
      <div className="login-card">
        <div className="brand-title">浙江工商大学本科生奖学金评选系统</div>
        <div className="brand-sub">信息与电子工程学院 · 综合测评与奖学金</div>

        <Alert
          type="info"
          showIcon
          message="登录账号即学校统一身份认证账号"
          description="学生使用学号，辅导员使用工号；初始密码与学校其他系统一致，登录后可在系统内自行修改。"
          style={{ marginBottom: 16 }}
        />

        <Form form={form} layout="vertical" onFinish={submit} initialValues={{ account: '', password: '' }}>
          <Form.Item name="account" label="账号" rules={[{ required: true, message: '请输入账号' }]}>
            <Input prefix={<UserOutlined />} placeholder="学号 / 工号" autoComplete="username" />
          </Form.Item>
          <Form.Item name="password" label="密码" rules={[{ required: true, message: '请输入密码' }]}>
            <Input.Password prefix={<LockOutlined />} placeholder="登录密码" autoComplete="current-password" />
          </Form.Item>
          <Button block type="primary" htmlType="submit" loading={loading}>登录</Button>
        </Form>

        <div style={{ marginTop: 24 }}>
          <div style={{ color: '#8c8c8c', marginBottom: 8 }}>演示账号（点击填入）：</div>
          <Space wrap>
            {samples.map((s) => (
              <Tag key={s.account} color="blue" style={{ cursor: 'pointer' }} onClick={() => fill(s)}>
                {s.label}
              </Tag>
            ))}
          </Space>
        </div>
      </div>
    </div>
  )
}
