import { useEffect, useState } from 'react'
import { Table, Tag, Card, Select, Space } from 'antd'
import api from '../../api.js'

const MAJORS = ['人工智能', '通信工程', '电子信息工程']

const numSorter = (path) => (a, b) => {
  const get = (obj, p) => p.reduce((o, k) => o?.[k], obj)
  return Number(get(a, path) || 0) - Number(get(b, path) || 0)
}

export default function Students() {
  const [list, setList] = useState([])
  const [major, setMajor] = useState(null)
  const [pageSize, setPageSize] = useState(20)

  const load = async (m) => {
    const params = m ? `?major=${encodeURIComponent(m)}` : ''
    setList(await api.get(`/counselor/students${params}`))
  }

  useEffect(() => { load(null) }, [])

  const handleMajorChange = (v) => {
    setMajor(v || null)
    load(v || null)
  }

  const cols = [
    {
      title: '学号', dataIndex: ['student', 'studentNo'], width: 110,
      sorter: (a, b) => (a.student?.studentNo || '').localeCompare(b.student?.studentNo || '')
    },
    { title: '姓名', dataIndex: ['student', 'name'], width: 80 },
    { title: '专业', dataIndex: ['student', 'major'], width: 130 },
    { title: '年级', dataIndex: ['student', 'grade'], width: 70 },
    {
      title: '培养类型', dataIndex: ['student', 'degreeType'], width: 90,
      render: (v) => v === 'ACADEMIC' ? <Tag color="purple">学术型</Tag> : <Tag color="blue">专业型</Tag>
    },
    {
      title: '德育 S1', dataIndex: ['evaluation', 'moralScore'], width: 90,
      sorter: numSorter(['evaluation', 'moralScore']),
      render: (v) => v ? Number(v).toFixed(2) : '—'
    },
    {
      title: '智育 S2', dataIndex: ['evaluation', 'academicScore'], width: 90,
      sorter: numSorter(['evaluation', 'academicScore']),
      render: (v) => v ? Number(v).toFixed(2) : '—'
    },
    {
      title: '素质 S3', dataIndex: ['evaluation', 'qualityScore'], width: 90,
      sorter: numSorter(['evaluation', 'qualityScore']),
      render: (v) => v ? Number(v).toFixed(2) : '—'
    },
    {
      title: '创新 S4', dataIndex: ['evaluation', 'innovationScore'], width: 90,
      sorter: numSorter(['evaluation', 'innovationScore']),
      render: (v) => v ? Number(v).toFixed(2) : '—'
    },
    {
      title: '总分', dataIndex: ['evaluation', 'totalScore'], width: 90,
      sorter: numSorter(['evaluation', 'totalScore']),
      render: (v) => v ? <b>{Number(v).toFixed(2)}</b> : '—'
    },
    {
      title: '排名', dataIndex: ['evaluation', 'rankOverall'], width: 80,
      sorter: (a, b) => (a.evaluation?.rankOverall || 9999) - (b.evaluation?.rankOverall || 9999),
      render: (v) => v ? `第 ${v} 名` : '—'
    },
    {
      title: '综测状态', dataIndex: ['evaluation', 'status'], width: 90,
      render: (v) => v === 'SUBMITTED' ? <Tag color="green">已提交</Tag> : <Tag>草稿</Tag>
    }
  ]

  return (
    <Card
      title="学生综测进度"
      extra={
        <Space>
          <span>专业：</span>
          <Select
            allowClear
            placeholder="全部专业"
            style={{ width: 160 }}
            value={major}
            onChange={handleMajorChange}
            options={MAJORS.map((m) => ({ value: m, label: m }))}
          />
        </Space>
      }
    >
      <Table
        size="small"
        rowKey={(r) => r.student.id}
        dataSource={list}
        columns={cols}
        scroll={{ x: 1300 }}
        pagination={{
          showSizeChanger: true,
          pageSizeOptions: ['10', '20', '50'],
          pageSize,
          onShowSizeChange: (_, size) => setPageSize(size),
          showTotal: (total) => `共 ${total} 人`
        }}
      />
    </Card>
  )
}
