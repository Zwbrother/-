import { useState, useEffect } from 'react'
import { Card, Form, Input, Select, Button, message, Table, Tag, Space } from 'antd'
import { SendOutlined } from '@ant-design/icons'
import api from '../../api.js'

export default function StudentAppeal() {
  const [form] = Form.useForm()
  const [appeals, setAppeals] = useState([])
  const [applications, setApplications] = useState([])
  const [loading, setLoading] = useState(false)
  const [submitting, setSubmitting] = useState(false)

  const load = async () => {
    setLoading(true)
    try {
      const [a, apps] = await Promise.all([
        api.get('/student/appeals'),
        api.get('/student/applications')
      ])
      setAppeals(a || [])
      setApplications(apps || [])
    } finally { setLoading(false) }
  }

  useEffect(() => { load() }, [])

  const handleSubmit = async (values) => {
    setSubmitting(true)
    try {
      await api.post('/student/appeals', values)
      message.success('申诉已提交，学院将在3个工作日内回复')
      form.resetFields()
      load()
    } finally { setSubmitting(false) }
  }

  const STATUS_MAP = {
    PENDING: { color: 'orange', label: '待处理' },
    PROCESSING: { color: 'blue', label: '处理中' },
    RESOLVED: { color: 'green', label: '已解决' },
    REJECTED: { color: 'red', label: '已驳回' }
  }

  const appColumns = [
    { title: '申诉级别', dataIndex: 'appealLevel', key: 'level', width: 100,
      render: v => v === 'COLLEGE' ? '向学院申诉' : '向学工部申诉' },
    { title: '申诉理由', dataIndex: 'reason', key: 'reason', ellipsis: true },
    { title: '状态', dataIndex: 'status', key: 'status', width: 100,
      render: v => { const s = STATUS_MAP[v]; return <Tag color={s?.color}>{s?.label || v}</Tag> } },
    { title: '回复', dataIndex: 'response', key: 'response', ellipsis: true,
      render: v => v || <span style={{ color: '#ccc' }}>等待回复</span> },
    { title: '提交时间', dataIndex: 'submittedAt', key: 'time', width: 170,
      render: v => v ? new Date(v).toLocaleString() : '-' }
  ]

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Card title="提交申诉">
        <Form form={form} layout="vertical" onFinish={handleSubmit}>
          <Form.Item label="关联申请" name="applicationId" rules={[{ required: true, message: '请选择关联的奖学金申请' }]}>
            <Select placeholder="选择要申诉的奖学金申请" allowClear>
              {applications.map(a => (
                <Select.Option key={a.application?.id} value={a.application?.id}>
                  [{a.project?.projectName}] {a.application?.status}
                </Select.Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item label="申诉级别" name="appealLevel" rules={[{ required: true }]}
            extra="首次申诉向学院提起；如对学院答复仍有异议，可在3个工作日内向学工部申诉">
            <Select>
              <Select.Option value="COLLEGE">向学院申诉</Select.Option>
              <Select.Option value="UNIVERSITY">向学工部申诉</Select.Option>
            </Select>
          </Form.Item>
          <Form.Item label="申诉理由" name="reason" rules={[{ required: true, message: '请填写申诉理由' }]}>
            <Input.TextArea rows={4} maxLength={1000} showCount
              placeholder="请详细说明申诉理由，包括对评审结果的异议内容" />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" icon={<SendOutlined />} loading={submitting}>
              提交申诉
            </Button>
          </Form.Item>
        </Form>
      </Card>

      <Card title="我的申诉记录">
        <Table columns={appColumns} dataSource={appeals} rowKey="id"
          loading={loading} pagination={{ pageSize: 10 }}
          locale={{ emptyText: '暂无申诉记录' }} />
      </Card>
    </Space>
  )
}
