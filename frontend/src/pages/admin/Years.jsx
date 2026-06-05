import { useEffect, useState } from 'react'
import { Card, Table, Button, Modal, Form, Input, DatePicker, message, Tag } from 'antd'
import dayjs from 'dayjs'
import api from '../../api.js'

export default function Years() {
  const [list, setList] = useState([])
  const [visible, setVisible] = useState(false)
  const [form] = Form.useForm()

  const load = async () => setList(await api.get('/admin/years'))
  useEffect(() => { load() }, [])

  const create = async () => {
    const v = await form.validateFields()
    const body = {
      yearName: v.yearName,
      startDate: v.dateRange?.[0]?.format('YYYY-MM-DD'),
      endDate: v.dateRange?.[1]?.format('YYYY-MM-DD'),
      status: 'ACTIVE'
    }
    await api.post('/admin/years', body)
    message.success('已创建')
    setVisible(false)
    form.resetFields()
    load()
  }

  const cols = [
    { title: '学年', dataIndex: 'yearName' },
    { title: '开始', dataIndex: 'startDate' },
    { title: '结束', dataIndex: 'endDate' },
    { title: '状态', dataIndex: 'status', render: (s) => <Tag color={s === 'ACTIVE' ? 'green' : 'default'}>{s}</Tag> }
  ]

  return (
    <Card title="学年管理" extra={<Button type="primary" onClick={() => setVisible(true)}>+ 新建学年</Button>}>
      <Table size="small" rowKey="id" dataSource={list} columns={cols} pagination={false} />
      <Modal open={visible} title="新建学年" onCancel={() => setVisible(false)} onOk={create} destroyOnClose>
        <Form form={form} layout="vertical">
          <Form.Item name="yearName" label="学年名" rules={[{ required: true }]}>
            <Input placeholder="例：2026-2027" />
          </Form.Item>
          <Form.Item name="dateRange" label="评奖学年区间" rules={[{ required: true }]}
                     extra="一般为 上年 9 月 1 日 ~ 当年 8 月 31 日">
            <DatePicker.RangePicker style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  )
}
