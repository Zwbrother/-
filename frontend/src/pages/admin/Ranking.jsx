import { useEffect, useState, useMemo } from 'react'
import { Card, Select, Table, Tag, Space } from 'antd'
import api from '../../api.js'

const MAJORS = ['人工智能', '通信工程', '电子信息工程']

export default function Ranking() {
  const [years, setYears] = useState([])
  const [yearId, setYearId] = useState(null)
  const [major, setMajor] = useState(null)
  const [degreeType, setDegreeType] = useState(null)
  const [rawList, setRawList] = useState([])
  const [pageSize, setPageSize] = useState(20)

  useEffect(() => {
    (async () => {
      const ys = await api.get('/admin/years')
      setYears(ys)
      const active = ys.find((y) => y.status === 'ACTIVE') || ys[0]
      if (active) setYearId(active.id)
    })()
  }, [])

  const load = async (yId) => {
    if (!yId) return
    setRawList(await api.get(`/admin/ranking?yearId=${yId}`))
  }

  useEffect(() => { load(yearId) }, [yearId])

  const list = useMemo(() => {
    let filtered = rawList
    if (major) filtered = filtered.filter((r) => r.student?.major === major)
    const sorted = [...filtered].sort((a, b) => {
      const ts = Number(b.evaluation?.basicTotal || 0) - Number(a.evaluation?.basicTotal || 0)
      return ts
    })
    return sorted.map((r, i) => ({ ...r, dynamicRank: i + 1 }))
  }, [rawList, major])

  const cols = [
    { title: '排名', width: 80, dataIndex: 'dynamicRank', render: (v) => {
      const color = v === 1 ? '#faad14' : v === 2 ? '#8c8c8c' : v === 3 ? '#cd7f32' : 'default'
      return <Tag color={color}>第 {v} 名</Tag>
    }},
    { title: '学号', dataIndex: ['student','studentNo'], width: 110,
      sorter: (a,b) => (a.student?.studentNo||'').localeCompare(b.student?.studentNo||'') },
    { title: '姓名', dataIndex: ['student', 'name'], width: 80 },
    { title: '专业', dataIndex: ['student', 'major'] },
    {
      title: '培养类型', dataIndex: ['student', 'degreeType'], width: 90,
      render: (v) => v === 'ACADEMIC' ? <Tag color="purple">学术型</Tag> : <Tag color="blue">专业型</Tag>
    },
    { title: '基本项分', dataIndex: ['evaluation','basicTotal'], sorter: (a,b) => Number(a.evaluation?.basicTotal||0) - Number(b.evaluation?.basicTotal||0), render: v => Number(v||0).toFixed(2) },
    { title: '加权均分', dataIndex: ['evaluation','academicWeightedAvg'], render: v => Number(v||0).toFixed(2) },
    { title: '能力分', dataIndex: ['evaluation','abilityTotal'], sorter: (a,b) => Number(a.evaluation?.abilityTotal||0) - Number(b.evaluation?.abilityTotal||0), render: v => Number(v||0).toFixed(2) }
  ]

  const filterLabel = [major].filter(Boolean).join(' · ')

  return (
    <Card
      title={
        <span>
          综合测评排名
          {filterLabel && <Tag color="blue" style={{ marginLeft: 12, fontWeight: 'normal' }}>{filterLabel}</Tag>}
          <span style={{ fontSize: 13, fontWeight: 'normal', color: '#8c8c8c', marginLeft: 8 }}>
            共 {list.length} 人
          </span>
        </span>
      }
      extra={
        <Space wrap>
          <span>学年：</span>
          <Select style={{ width: 170 }} value={yearId} onChange={(v) => setYearId(v)}
                  options={years.map((y) => ({ value: y.id, label: y.yearName }))} />
          <span>专业：</span>
          <Select allowClear placeholder="全部" style={{ width: 140 }} value={major}
                  onChange={(v) => setMajor(v || null)}
                  options={MAJORS.map((m) => ({ value: m, label: m }))} />
          <span>年级：</span>
          <Select allowClear placeholder="全部" style={{ width: 120 }} value={degreeType}
                  onChange={(v) => setDegreeType(v || null)}
                  options={[{ value: '大一', label: '大一' }, { value: '大二', label: '大二' }, { value: '大三', label: '大三' }, { value: '大四', label: '大四' }]} />
        </Space>
      }
    >
      <Table
        size="small"
        rowKey={(r) => r.evaluation?.id ?? r.student?.id}
        dataSource={list}
        columns={cols}
        scroll={{ x: 1100 }}
        pagination={{
          showSizeChanger: true,
          pageSizeOptions: ['10', '20', '50', '100'],
          pageSize,
          onShowSizeChange: (_, size) => setPageSize(size),
          showTotal: (total) => `共 ${total} 人`
        }}
      />
    </Card>
  )
}
