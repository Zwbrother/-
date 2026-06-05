import { useEffect, useState } from 'react'
import { Table, Tag, Popconfirm, Button, message, Card } from 'antd'
import api from '../../api.js'

const STATUS = {
  SUBMITTED: ['gold', '待审核'], REVIEWING: ['blue', '审核中'],
  APPROVED: ['green', '已通过'], REJECTED: ['red', '已退回'],
  WITHDRAWN: ['default', '已撤回'], PUBLISHED: ['purple', '已公示']
}

export default function Applications() {
  const [list, setList] = useState([])
  const load = async () => setList(await api.get('/student/applications'))
  useEffect(() => { load() }, [])

  const withdraw = async (id) => {
    await api.delete(`/student/applications/${id}`)
    message.success('已撤回'); load()
  }

  const cols = [
    { title: '奖学金项目', dataIndex: ['project','projectName'] },
    { title: '类型', dataIndex: ['project','typeCode'] },
    { title: '基本分', dataIndex: ['application','snapshotBasicTotal'], render: v => v ? Number(v).toFixed(2) : '—' },
    { title: '能力分', dataIndex: ['application','snapshotAbilityTotal'], render: v => v ? Number(v).toFixed(2) : '—' },
    { title: '能力排名', dataIndex: ['application','snapshotAbilityRank'], render: v => v ? `第${v}名` : '—' },
    { title: '系统推荐', dataIndex: ['autoLevel','levelName'], render: v => v ? <Tag color="gold">{v}</Tag> : '—' },
    { title: '最终授予', dataIndex: ['finalLevel','levelName'], render: v => v ? <Tag color="green">{v}</Tag> : '—' },
    { title: '状态', dataIndex: ['application','status'], render: s => {
      const [color, text] = STATUS[s] || ['default', s]; return <Tag color={color}>{text}</Tag>
    }},
    { title: '退回原因', dataIndex: ['application','rejectReason'], render: v => v || '—' },
    { title: '操作', render: (_, r) => r.application.status === 'SUBMITTED'
      ? <Popconfirm title="撤回？" onConfirm={() => withdraw(r.application.id)}><a>撤回</a></Popconfirm> : null }
  ]
  return (
    <Card title="我的申请">
      <Table size="small" rowKey={r => r.application.id} dataSource={list} columns={cols} pagination={false} />
    </Card>
  )
}
