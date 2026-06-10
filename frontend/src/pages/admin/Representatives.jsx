import { useState, useEffect } from 'react'
import { Table, Button, Modal, Select, message, Space, Tag, Statistic, Row, Col, Card, Popconfirm } from 'antd'
import { PlusOutlined, DeleteOutlined, CheckCircleOutlined, CloseCircleOutlined } from '@ant-design/icons'
import api from '../../api.js'

export default function AdminRepresentatives() {
  const [reps, setReps] = useState([])
  const [students, setStudents] = useState([])
  const [years, setYears] = useState([])
  const [loading, setLoading] = useState(false)
  const [modalOpen, setModalOpen] = useState(false)
  const [selectedStudent, setSelectedStudent] = useState(null)
  const [selectedYear, setSelectedYear] = useState(null)
  const [ratio, setRatio] = useState(null)

  const fetchReps = async () => {
    setLoading(true)
    try {
      const data = await api.get('/admin/representatives', { params: { yearId: selectedYear } })
      setReps(data || [])
    } finally { setLoading(false) }
  }

  const fetchYears = async () => {
    try {
      const data = await api.get('/admin/years')
      setYears(data || [])
      if (data?.length > 0) setSelectedYear(data[0].id)
    } catch { /* ignore */ }
  }

  const fetchStudents = async () => {
    try {
      const data = await api.get('/admin/ranking', { params: { yearId: selectedYear } })
      const all = (data || []).map(r => r.student).filter(Boolean)
      setStudents(all)
    } catch { /* ignore */ }
  }

  const fetchRatio = async () => {
    if (!selectedYear) return
    try {
      const data = await api.get('/admin/representatives/check-ratio', { params: { yearId: selectedYear } })
      setRatio(data)
    } catch { /* ignore */ }
  }

  useEffect(() => { fetchYears() }, [])
  useEffect(() => { if (selectedYear) { fetchReps(); fetchStudents(); fetchRatio() } }, [selectedYear])

  const handleAdd = async () => {
    if (!selectedStudent || !selectedYear) { message.warning('请选择学生和学年'); return }
    try {
      const stu = students.find(s => s.id === selectedStudent)
      await api.post('/admin/representatives', {
        studentId: selectedStudent,
        academicYearId: selectedYear,
        className: stu?.className || ''
      })
      message.success('学生代表添加成功')
      setModalOpen(false)
      setSelectedStudent(null)
      fetchReps()
      fetchRatio()
    } catch { /* api handles error */ }
  }

  const handleRemove = async (id) => {
    try {
      await api.delete(`/admin/representatives/${id}`)
      message.success('已移除')
      fetchReps()
      fetchRatio()
    } catch { /* api handles error */ }
  }

  const columns = [
    { title: '学号', dataIndex: ['student', 'studentNo'], key: 'no' },
    { title: '姓名', dataIndex: ['student', 'name'], key: 'name' },
    { title: '专业', dataIndex: ['student', 'major'], key: 'major' },
    { title: '班级', dataIndex: ['student', 'className'], key: 'class' },
    {
      title: '操作', key: 'actions', width: 100,
      render: (_, r) => (
        <Popconfirm title="确定移除该学生代表？" onConfirm={() => handleRemove(r.rep.id)}>
          <Button type="link" danger icon={<DeleteOutlined />}>移除</Button>
        </Popconfirm>
      )
    }
  ]

  const existingIds = new Set(reps.map(r => r.rep?.studentId))

  return (
    <div>
      <h2 style={{ marginBottom: 16 }}>学生代表管理</h2>

      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={6}>
          <Card size="small">
            <Statistic title="代表人数" value={reps.length} suffix="人" />
          </Card>
        </Col>
        <Col span={6}>
          <Card size="small">
            <Statistic title="学生总数" value={ratio?.totalStudents || 0} suffix="人" />
          </Card>
        </Col>
        <Col span={6}>
          <Card size="small">
            <Statistic title="代表比例" value={ratio?.ratio || 0} suffix="%"
              valueStyle={{ color: ratio?.meetsRequirement ? '#3f8600' : '#cf1322' }} />
          </Card>
        </Col>
        <Col span={6}>
          <Card size="small">
            <Statistic title="是否符合≥30%要求" value={ratio?.meetsRequirement ? '✅ 符合' : '❌ 不符合'}
              valueStyle={{ color: ratio?.meetsRequirement ? '#3f8600' : '#cf1322' }} />
          </Card>
        </Col>
      </Row>

      <Space style={{ marginBottom: 16 }}>
        <Select placeholder="选择学年" style={{ width: 200 }} value={selectedYear}
          onChange={setSelectedYear}
          options={years.map(y => ({ label: y.yearName, value: y.id }))} />
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setModalOpen(true)}>
          新增代表
        </Button>
      </Space>

      <Table columns={columns} dataSource={reps} rowKey={r => r.rep?.id}
        loading={loading} pagination={{ pageSize: 20 }} />

      <Modal title="新增学生代表" open={modalOpen} onOk={handleAdd}
        onCancel={() => { setModalOpen(false); setSelectedStudent(null) }}
        okText="确认添加" cancelText="取消">
        <Select showSearch placeholder="搜索并选择学生" style={{ width: '100%' }}
          value={selectedStudent} onChange={setSelectedStudent}
          filterOption={(input, option) => option.label.includes(input)}
          options={students
            .filter(s => !existingIds.has(s.id))
            .map(s => ({ label: `${s.studentNo} - ${s.name} (${s.major})`, value: s.id }))} />
      </Modal>
    </div>
  )
}
