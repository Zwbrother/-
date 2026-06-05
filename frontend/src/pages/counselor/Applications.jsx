import { useEffect, useState } from 'react'
import { Card, Table, Tag, Button, Space, Modal, Input, message, Select } from 'antd'
import api from '../../api.js'

const STATUS = {
  SUBMITTED: ['gold', '待审'],
  APPROVED: ['green', '已通过'],
  REJECTED: ['red', '已退回'],
  PUBLISHED: ['purple', '已公示']
}

export default function Applications() {
  const [list, setList] = useState([])
  const [status, setStatus] = useState('')
  const [selected, setSelected] = useState([])

  const load = async () => setList(await api.get(`/counselor/applications${status ? `?status=${status}` : ''}`))
  useEffect(() => { load() }, [status])

  const approve = async (id) => {
    await api.post(`/counselor/applications/${id}/review`, { status: 'APPROVED' })
    message.success('已通过')
    load()
  }
  const reject = (id) => {
    let reason = ''
    Modal.confirm({
      title: '请填写退回原因',
      content: <Input onChange={(e) => reason = e.target.value} />,
      onOk: async () => {
        await api.post(`/counselor/applications/${id}/review`, { status: 'REJECTED', reason })
        message.success('已退回')
        load()
      }
    })
  }
  const batchApprove = async () => {
    if (selected.length === 0) return message.warning('请先勾选')
    await api.post('/counselor/applications/batch-review', { ids: selected })
    message.success(`批量通过 ${selected.length} 条`)
    setSelected([])
    load()
  }

  const cols = [
    { title: '学生', render: (_, r) => `${r.student.name}（${r.student.studentNo}）` },
    { title: '奖学金项目', dataIndex: ['project', 'projectName'] },
    { title: '综测总分', dataIndex: ['application', 'snapshotTotalScore'], render: (v) => v ? Number(v).toFixed(2) : '—' },
    { title: '综测排名', dataIndex: ['application', 'snapshotRank'], render: (v) => v ? `第 ${v} 名` : '—' },
    { title: '系统推荐', dataIndex: ['autoLevel', 'levelName'], render: (v) => v ? <Tag color="gold">{v}</Tag> : '—' },
    { title: '最终授予', dataIndex: ['finalLevel', 'levelName'], render: (v) => v ? <Tag color="green">{v}</Tag> : '—' },
    {
      title: '状态', dataIndex: ['application', 'status'], render: (s) => {
        const [c, t] = STATUS[s] || ['default', s]
        return <Tag color={c}>{t}</Tag>
      }
    },
    {
      title: '操作', render: (_, r) => r.application.status === 'SUBMITTED' && (
        <Space>
          <Button size="small" type="primary" onClick={() => approve(r.application.id)}>通过</Button>
          <Button size="small" danger onClick={() => reject(r.application.id)}>退回</Button>
        </Space>
      )
    }
  ]

  return (
    <Card title="奖学金申请审核"
          extra={<Space>
            <span>筛选状态：</span>
            <Select style={{ width: 140 }} value={status} onChange={setStatus} options={[
              { value: '', label: '全部' },
              { value: 'SUBMITTED', label: '待审' },
              { value: 'APPROVED', label: '已通过' },
              { value: 'REJECTED', label: '已退回' }
            ]} />
            <Button type="primary" onClick={batchApprove} disabled={selected.length === 0}>批量通过 ({selected.length})</Button>
          </Space>}>
      <Table
        size="small"
        rowKey={(r) => r.application.id}
        dataSource={list}
        columns={cols}
        pagination={{
          showSizeChanger: true,
          pageSizeOptions: ['10', '20', '50'],
          defaultPageSize: 20,
          showTotal: (total) => `共 ${total} 条`
        }}
        rowSelection={{
          selectedRowKeys: selected,
          onChange: setSelected,
          getCheckboxProps: (r) => ({ disabled: r.application.status !== 'SUBMITTED' })
        }}
      />
    </Card>
  )
}
