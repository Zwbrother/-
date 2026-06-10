import { Form, Input, Button, Card, message } from 'antd'
import api from '../api.js'
import { useAuthStore } from '../store.js'

export default function ChangePassword() {
  const [form] = Form.useForm()
  const user = useAuthStore((s) => s.user)
  const setAuth = useAuthStore((s) => s.setAuth)

  const submit = async (v) => {
    if (v.newPassword !== v.confirm) return message.error('两次输入的新密码不一致')
    await api.post('/auth/change-password', { oldPassword: v.oldPassword, newPassword: v.newPassword })
    message.success('密码已修改')
    setAuth(useAuthStore.getState().token, { ...user, usingInitialPassword: false })
    form.resetFields()
  }

  return (
    <Card title="修改登录密码" style={{ maxWidth: 600 }}>
      <Form form={form} layout="vertical" onFinish={submit}>
        <Form.Item name="oldPassword" label="原密码" rules={[{ required: true }]}>
          <Input.Password autoComplete="current-password" />
        </Form.Item>
        <Form.Item name="newPassword" label="新密码" rules={[{ required: true, min: 6, message: '至少 6 位' }]}>
          <Input.Password autoComplete="new-password" />
        </Form.Item>
        <Form.Item name="confirm" label="确认新密码" rules={[{ required: true }]}>
          <Input.Password autoComplete="new-password" />
        </Form.Item>
        <Button type="primary" htmlType="submit">提交</Button>
      </Form>
    </Card>
  )
}
