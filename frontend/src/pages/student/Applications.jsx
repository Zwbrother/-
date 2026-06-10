import { useEffect, useState } from 'react'
import { Table, Tag, Popconfirm, Button, message, Card } from 'antd'
import api from '../../api.js'

const STATUS = {
  SUBMITTED: ['gold', '待审核'], REVIEWING: ['blue', '审核中'],
  APPROVED: ['green', '已通过'], REJECTED: ['red', '已退回'],
  WITHDRAWN: ['default', '已撤回'], PUBLISHED: ['purple', '已公示']
}

const GE_STATUS = { SUBMITTED: ['gold','待审核'], APPROVED: ['green','已通过'], REJECTED: ['red','已退回'] }

export default function Applications() {
  const [list, setList] = useState([])
  const load = async () => setList(await api.get('/student/applications'))
  useEffect(() => { load() }, [])

  const withdraw = async (id, isGE) => {
    await api.delete(isGE ? `/student/graduate-exam/${id}` : `/student/applications/${id}`)
    message.success('已撤回'); load()
  }

  const cols = [
    { title: '奖学金项目', dataIndex: ['project','projectName'] },
    { title: '类型', dataIndex: ['project','typeCode'] },
    { title: '基本分', render: (_, r) => r.isGraduateExam ? '—' : (r.application?.snapshotBasicTotal ? Number(r.application.snapshotBasicTotal).toFixed(2) : '—') },
    { title: '能力分', render: (_, r) => r.isGraduateExam ? '—' : (r.application?.snapshotAbilityTotal ? Number(r.application.snapshotAbilityTotal).toFixed(2) : '—') },
    { title: '能力排名', render: (_, r) => r.isGraduateExam ? '—' : (r.application?.snapshotAbilityRank ? `第${r.application.snapshotAbilityRank}名` : '—') },
    { title: '详情', render: (_, r) => {
      if (!r.isGraduateExam) return '—'
      const a = r.application
      return (
        <span style={{fontSize:12}}>
          {a.examType === 'DOMESTIC' ? '国内考研' : '国外申研'}｜{a.schoolName || '—'}
          <br/>
          复试：{a.hasInterviewQualification ? <Tag color="blue" style={{margin:0}}>通过</Tag> : <Tag style={{margin:0}}>未通过</Tag>}
          &nbsp;录取：{a.isAdmitted ? <Tag color="green" style={{margin:0}}>已录取</Tag> : <Tag style={{margin:0}}>未录取</Tag>}
        </span>
      )
    }},
    { title: '系统推荐', dataIndex: ['autoLevel','levelName'], render: v => v ? <Tag color="gold">{v}</Tag> : '—' },
    { title: '最终授予', dataIndex: ['finalLevel','levelName'], render: v => v ? <Tag color="green">{v}</Tag> : '—' },
    { title: '状态', render: (_, r) => {
      const s = r.application?.status
      const [color, text] = r.isGraduateExam ? (GE_STATUS[s] || ['default',s]) : (STATUS[s] || ['default',s])
      return <Tag color={color}>{text}</Tag>
    }},
    { title: '退回原因', render: (_, r) => {
      const reason = r.application?.rejectReason
      if (!reason) return '—'
      return <span style={{color:'#cf1322'}}>{reason}</span>
    }},
    { title: '操作', render: (_, r) => (
      r.application?.status === 'SUBMITTED'
        ? <Popconfirm title="撤回？" onConfirm={() => withdraw(r.application.id, r.isGraduateExam)}><a>撤回</a></Popconfirm> : null
    )}
  ]
  return (
    <Card title="我的申请">
      <Table size="small" rowKey={r => r.isGraduateExam ? `ge-${r.application.id}` : r.application.id} dataSource={list} columns={cols} pagination={false} />
    </Card>
  )
}
