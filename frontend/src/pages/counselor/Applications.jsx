import { useEffect, useState } from 'react'
import { Card, Table, Tag, Button, Space, Modal, Input, message, Select, Tabs, Image, Radio } from 'antd'
import { EyeOutlined } from '@ant-design/icons'
import api from '../../api.js'

const STATUS = {
  SUBMITTED: ['gold', '待审'],
  APPROVED: ['green', '已通过'],
  REJECTED: ['red', '已退回'],
  PUBLISHED: ['purple', '已公示']
}

const GE_STATUS = { SUBMITTED: ['gold','待审'], APPROVED: ['green','已通过'], REJECTED: ['red','已退回'] }

export default function Applications() {
  const [list, setList] = useState([])
  const [geList, setGeList] = useState([])
  const [status, setStatus] = useState('')
  const [selected, setSelected] = useState([])
  const [preview, setPreview] = useState({ visible: false, url: '' })

  const load = async () => setList(await api.get(`/counselor/applications${status ? `?status=${status}` : ''}`))
  const loadGE = async () => setGeList(await api.get('/counselor/graduate-exam'))
  useEffect(() => { load(); loadGE() }, [status])

  const approve = async (id) => {
    await api.post(`/counselor/applications/${id}/review`, { status: 'APPROVED' })
    message.success('已通过'); load()
  }
  const reject = (id) => {
    let reason = ''
    Modal.confirm({
      title: '请填写退回原因',
      content: <Input onChange={(e) => reason = e.target.value} placeholder="退回原因（必填）" />,
      onOk: async () => {
        if (!reason.trim()) { message.warning('请填写退回原因'); return Promise.reject() }
        await api.post(`/counselor/applications/${id}/review`, { status: 'REJECTED', reason })
        message.success('已退回'); load()
      }
    })
  }
  const batchApprove = async () => {
    if (selected.length === 0) return message.warning('请先勾选')
    await api.post('/counselor/applications/batch-review', { ids: selected })
    message.success(`批量通过 ${selected.length} 条`)
    setSelected([]); load()
  }

  // 考研奖学金审核
  const approveGE = (id, currentLevel) => {
    let finalLevel = currentLevel
    Modal.confirm({
      title: '确认通过考研奖学金审核',
      content: <div>
        <p>系统判定等级：{currentLevel === 'FIRST' ? '一等奖 600元' : '二等奖 300元'}</p>
        <div style={{ marginTop: 8 }}>调整等级（可选）：<Radio.Group defaultValue={currentLevel} onChange={e => finalLevel = e.target.value}>
          <Radio value='FIRST'>一等奖 600元</Radio>
          <Radio value='SECOND'>二等奖 300元</Radio>
        </Radio.Group></div>
      </div>,
      onOk: async () => {
        await api.post(`/counselor/graduate-exam/${id}/review`, { status: 'APPROVED', finalLevel })
        message.success('已通过'); loadGE()
      }
    })
  }
  const rejectGE = (id) => {
    let reason = ''
    Modal.confirm({
      title: '请填写退回原因',
      content: <Input onChange={(e) => reason = e.target.value} placeholder="退回原因（必填）" />,
      onOk: async () => {
        if (!reason.trim()) { message.warning('请填写退回原因'); return Promise.reject() }
        await api.post(`/counselor/graduate-exam/${id}/review`, { status: 'REJECTED', reason })
        message.success('已退回'); loadGE()
      }
    })
  }

  const appCols = [
    { title: '学生', render: (_, r) => `${r.student.name}（${r.student.studentNo}）` },
    { title: '奖学金项目', dataIndex: ['project', 'projectName'] },
    { title: '基本分', dataIndex: ['application', 'snapshotBasicTotal'], render: (v) => v ? Number(v).toFixed(2) : '—' },
    { title: '能力分', dataIndex: ['application', 'snapshotAbilityTotal'], render: (v) => v ? Number(v).toFixed(2) : '—' },
    { title: '系统推荐', dataIndex: ['autoLevel', 'levelName'], render: (v) => v ? <Tag color="gold">{v}</Tag> : '—' },
    { title: '最终授予', dataIndex: ['finalLevel', 'levelName'], render: (v) => v ? <Tag color="green">{v}</Tag> : '—' },
    { title: '状态', render: (_, r) => { const [c, t] = STATUS[r.application.status] || ['default', r.application.status]; return <Tag color={c}>{t}</Tag> }},
    { title: '操作', render: (_, r) => r.application.status === 'SUBMITTED' && (
      <Space>
        <Button size="small" type="primary" onClick={() => approve(r.application.id)}>通过</Button>
        <Button size="small" danger onClick={() => reject(r.application.id)}>退回</Button>
      </Space>
    )}
  ]

  const geCols = [
    { title: '学生', render: (_, r) => `${r.student?.name}（${r.student?.studentNo}）` },
    { title: '考试类型', render: (_, r) => r.application?.examType === 'DOMESTIC' ? '国内考研' : '国外申研' },
    { title: '报考学校', dataIndex: ['application','schoolName'] },
    { title: '报考专业', dataIndex: ['application','majorName'] },
    { title: '复试资格', render: (_, r) => r.application?.hasInterviewQualification ? <Tag color="blue">已获得</Tag> : <Tag>未获得</Tag> },
    { title: '录取状态', render: (_, r) => r.application?.isAdmitted ? <Tag color="green">已录取</Tag> : <Tag>未录取</Tag> },
    { title: '附件', width: 70, render: (_, r) => r.application?.attachmentUrl
      ? <Button size="small" icon={<EyeOutlined />} onClick={() => setPreview({visible:true,url:r.application.attachmentUrl})}>查看</Button>
      : <Tag color="orange">无</Tag> },
    { title: '系统判定', render: (_, r) => {
      const lv = r.application?.finalLevel
      return lv ? <Tag color="gold">{lv === 'FIRST' ? '一等奖 600元' : '二等奖 300元'}</Tag> : '—'
    }},
    { title: '状态', render: (_, r) => { const [c, t] = GE_STATUS[r.application?.status] || ['default', r.application?.status]; return <Tag color={c}>{t}</Tag> }},
    { title: '操作', render: (_, r) => {
      const s = r.application?.status
      return (s === 'SUBMITTED' || s === 'APPROVED') && (
        <Space>
          {s === 'SUBMITTED' && <Button size="small" type="primary" onClick={() => approveGE(r.application.id, r.application?.finalLevel)}>通过</Button>}
          <Button size="small" danger onClick={() => rejectGE(r.application.id)}>退回</Button>
        </Space>
      )
    }}
  ]

  const PAGE_CFG = { showSizeChanger: true, pageSizeOptions: ['10','20','50'], defaultPageSize: 20, showTotal: total => `共 ${total} 条` }

  return (
    <Card title="奖学金申请审核">
      <Tabs defaultActiveKey="regular" items={[
        { key: 'regular', label: `常规申请（${list.length}）`, children: (
          <Card size="small" extra={<Space>
            <span>筛选状态：</span>
            <Select style={{ width: 140 }} value={status} onChange={setStatus} options={[
              { value: '', label: '全部' }, { value: 'SUBMITTED', label: '待审' },
              { value: 'APPROVED', label: '已通过' }, { value: 'REJECTED', label: '已退回' }
            ]} />
            <Button type="primary" onClick={batchApprove} disabled={selected.length === 0}>批量通过 ({selected.length})</Button>
          </Space>}>
            <Table size="small" rowKey={r => r.application.id} dataSource={list} columns={appCols} pagination={PAGE_CFG}
                   rowSelection={{ selectedRowKeys: selected, onChange: setSelected, getCheckboxProps: r => ({ disabled: r.application.status !== 'SUBMITTED' }) }} />
          </Card>
        )},
        { key: 'graduate', label: `考研奖学金（${geList.length}）`, children: (
          <Table size="small" rowKey={r => r.application.id} dataSource={geList} columns={geCols} pagination={PAGE_CFG} />
        )}
      ]} />
      <Modal open={preview.visible} title="证明材料" footer={null}
             onCancel={() => setPreview({visible:false,url:''})} width={700} centered>
        {preview.url && (preview.url.match(/\.(jpg|jpeg|png|gif|webp|bmp)$/i)
          ? <Image src={preview.url} style={{width:'100%',maxHeight:'70vh',objectFit:'contain'}} preview={false} />
          : <div style={{textAlign:'center',padding:24}}><a href={preview.url} target="_blank" rel="noreferrer">打开文件</a></div>)}
      </Modal>
    </Card>
  )
}
