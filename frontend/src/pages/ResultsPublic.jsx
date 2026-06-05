import { useEffect, useState } from 'react'
import { Card, Table, Input, Tag, Button, Empty } from 'antd'
import { useNavigate } from 'react-router-dom'
import api from '../api.js'

export default function ResultsPublic() {
  const [keyword, setKeyword] = useState('')
  const [list, setList] = useState([])
  const navigate = useNavigate()

  const load = async () => setList(await api.get(`/public/results${keyword ? `?keyword=${encodeURIComponent(keyword)}` : ''}`))
  useEffect(() => { load() }, [])

  const cols = [
    { title: '学号', dataIndex: 'studentNo' },
    { title: '姓名', dataIndex: 'name' },
    { title: '学院', dataIndex: 'college' },
    { title: '专业', dataIndex: 'major' },
    { title: '奖学金', dataIndex: 'projectName' },
    { title: '等级', dataIndex: 'levelName', render: v => v ? <Tag color="gold">{v}</Tag> : '—' },
    { title: '金额', dataIndex: 'amount', render: v => v ? `${v} 元` : '—' },
    { title: '基本分', dataIndex: 'basicTotal', render: v => v ? Number(v).toFixed(2) : '—' },
    { title: '能力分', dataIndex: 'abilityTotal', render: v => v ? Number(v).toFixed(2) : '—' },
    { title: '能力排名', dataIndex: 'abilityRank', render: v => v ? `第${v}名` : '—' }
  ]

  return (
    <div style={{ padding: 24, maxWidth: 1200, margin: '0 auto' }}>
      <Card title="本科生奖学金获奖名单公示" extra={<Button onClick={() => navigate('/login')}>返回登录</Button>}>
        <div style={{ marginBottom: 12 }}>
          <Input.Search placeholder="搜索学号 / 姓名 / 奖学金名称" allowClear style={{ width: 360 }}
                        value={keyword} onChange={e => setKeyword(e.target.value)} onSearch={load} />
        </div>
        {list.length === 0
          ? <Empty description="暂无公示数据。" />
          : <Table size="small" rowKey={(r,i) => i} dataSource={list} columns={cols} pagination={false} scroll={{ x: 1000 }} />}
      </Card>
    </div>
  )
}
