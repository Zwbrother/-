import { useState, useEffect } from 'react'
import { Table, Button, InputNumber, Input, Select, message, Space, Modal, Descriptions } from 'antd'
import { SaveOutlined, SearchOutlined } from '@ant-design/icons'
import api from '../../api.js'

const DIMENSIONS = [
  { key: 'politicalLiteracy', label: '政治素养', max: 20 },
  { key: 'legalAwareness', label: '法治意识', max: 20 },
  { key: 'mentalQuality', label: '心理素质', max: 20 },
  { key: 'integrityScore', label: '诚信品德', max: 20 },
  { key: 'teamwork', label: '团队协作', max: 20 },
  { key: 'socialResponsibility', label: '社会责任', max: 20 }
]

export default function CounselorAppraisal() {
  const [students, setStudents] = useState([])
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [scores, setScores] = useState({})
  const [major, setMajor] = useState('')
  const [className, setClassName] = useState('')
  const [previewOpen, setPreviewOpen] = useState(false)
  const [previewStudent, setPreviewStudent] = useState(null)

  const fetchStudents = async () => {
    setLoading(true)
    try {
      const data = await api.get('/counselor/batch-appraisal/students', {
        params: { major: major || undefined, className: className || undefined }
      })
      setStudents(data || [])
      const init = {}
      ;(data || []).forEach(row => {
        const sId = row.student.id
        if (row.appraisal) {
          init[sId] = { ...row.appraisal }
        } else {
          init[sId] = { studentId: sId }
          DIMENSIONS.forEach(d => { init[sId][d.key] = 0 })
        }
      })
      setScores(init)
    } finally { setLoading(false) }
  }

  useEffect(() => { fetchStudents() }, [])

  const updateScore = (studentId, key, value) => {
    setScores(prev => ({
      ...prev,
      [studentId]: { ...(prev[studentId] || { studentId }), [key]: value || 0 }
    }))
  }

  const handleSaveOne = async (studentId) => {
    setSaving(true)
    try {
      await api.post('/counselor/batch-appraisal', {
        appraisals: [scores[studentId]]
      })
      message.success('评议已保存')
      fetchStudents()
    } finally { setSaving(false) }
  }

  const handleSaveAll = async () => {
    setSaving(true)
    try {
      const appraisals = students.map(s => scores[s.student.id]).filter(Boolean)
      await api.post('/counselor/batch-appraisal', { appraisals })
      message.success(`批量评议完成，共 ${appraisals.length} 人`)
      fetchStudents()
    } finally { setSaving(false) }
  }

  const columns = [
    { title: '学号', dataIndex: ['student', 'studentNo'], key: 'no', width: 120 },
    { title: '姓名', dataIndex: ['student', 'name'], key: 'name', width: 80 },
    { title: '班级', dataIndex: ['student', 'className'], key: 'class', width: 100 },
    ...DIMENSIONS.map(d => ({
      title: d.label,
      key: d.key,
      width: 110,
      render: (_, row) => {
        const sId = row.student.id
        return (
          <InputNumber size="small" min={0} max={d.max} step={0.1}
            value={scores[sId]?.[d.key] || 0}
            onChange={v => updateScore(sId, d.key, v)}
            style={{ width: 80 }} />
        )
      }
    })),
    {
      title: '总分', key: 'total', width: 60,
      render: (_, row) => {
        const s = scores[row.student.id]
        if (!s) return '-'
        return DIMENSIONS.reduce((sum, d) => sum + (Number(s[d.key]) || 0), 0).toFixed(1)
      }
    },
    {
      title: '操作', key: 'actions', width: 120,
      render: (_, row) => (
        <Space>
          <Button size="small" onClick={() => { setPreviewStudent(row); setPreviewOpen(true) }}>
            详情
          </Button>
          <Button size="small" type="primary" icon={<SaveOutlined />}
            loading={saving} onClick={() => handleSaveOne(row.student.id)}>
            保存
          </Button>
        </Space>
      )
    }
  ]

  return (
    <div>
      <h2 style={{ marginBottom: 16 }}>辅导员批量评议</h2>

      <Space style={{ marginBottom: 16 }}>
        <Input placeholder="专业筛选" value={major} onChange={e => setMajor(e.target.value)}
          style={{ width: 150 }} allowClear />
        <Input placeholder="班级筛选" value={className} onChange={e => setClassName(e.target.value)}
          style={{ width: 150 }} allowClear />
        <Button icon={<SearchOutlined />} onClick={fetchStudents}>筛选</Button>
        <Button type="primary" icon={<SaveOutlined />} onClick={handleSaveAll}
          loading={saving}>一键批量保存</Button>
      </Space>

      <Table columns={columns} dataSource={students}
        rowKey={r => r.student?.id} loading={loading}
        pagination={{ pageSize: 15 }}
        scroll={{ x: 1200 }} />

      <Modal title={`${previewStudent?.student?.name || ''} - 评议详情`}
        open={previewOpen} onCancel={() => setPreviewOpen(false)} footer={null}>
        {previewStudent && scores[previewStudent.student.id] && (
          <Descriptions column={2} bordered size="small">
            {DIMENSIONS.map(d => (
              <Descriptions.Item key={d.key} label={d.label}>
                {scores[previewStudent.student.id][d.key] || 0} / {d.max}
              </Descriptions.Item>
            ))}
            <Descriptions.Item label="总分">
              {DIMENSIONS.reduce((sum, d) =>
                sum + (Number(scores[previewStudent.student.id][d.key]) || 0), 0).toFixed(1)}
            </Descriptions.Item>
          </Descriptions>
        )}
      </Modal>
    </div>
  )
}
