import { useEffect, useState } from 'react'
import { Card, List, Tag, Button, Empty, Space, Descriptions, message, Modal, Alert } from 'antd'
import { TrophyOutlined } from '@ant-design/icons'
import api from '../../api.js'

const TYPE_LABELS = {
  COMPREHENSIVE: '优秀学生综合奖学金', ABILITY: '能力突出奖学金',
  GRADUATE_EXAM: '考研奖学金', SPECIAL: '单项奖学金', NATIONAL: '国家奖学金',
  PROVINCIAL: '省政府奖学金', NAMED: '专项奖学金'
}

export default function Scholarships() {
  const [list, setList] = useState([])
  const load = async () => setList(await api.get('/student/scholarships/eligible'))
  useEffect(() => { load() }, [])

  const apply = (projectId, recommend, eligibility) => {
    if (eligibility) {
      Modal.warning({ title: '不符合申报条件', content: eligibility })
      return
    }
    Modal.confirm({
      title: '提交奖学金申请？',
      content: recommend
        ? `系统将依据您综合能力排名推荐：${recommend.levelName}（${recommend.amount} 元）。最终等级由辅导员审核确认。`
        : '提交后等待管理员触发排名计算。',
      onOk: async () => {
        await api.post('/student/applications', { projectId })
        message.success('已提交申请')
        load()
      }
    })
  }

  if (list.length === 0) return <Empty description="当前无可申报奖学金，请等待管理员发布。" />

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      {list.map((row) => {
        const p = row.project
        const recommend = row.recommendLevelId
          ? row.levels.find((l) => l.id === row.recommendLevelId) : null
        const applied = !!row.application
        const eligibility = row.eligibilityCheck
        return (
          <Card key={p.id}
                title={<Space><TrophyOutlined style={{ color: '#faad14' }} />
                  <span>{p.projectName}</span><Tag>{TYPE_LABELS[p.typeCode] || p.typeCode}</Tag>
                </Space>}
                extra={
                  applied
                    ? <Tag color="green">已申报</Tag>
                    : <Button type="primary" disabled={!!eligibility}
                              onClick={() => apply(p.id, recommend, eligibility)}>
                        {eligibility ? '不符合条件' : '立即申报'}
                      </Button>}>
            {eligibility && <Alert type="warning" message={eligibility} showIcon style={{ marginBottom: 12 }} />}
            <Descriptions column={2} size="small">
              <Descriptions.Item label="状态"><Tag>{p.status}</Tag></Descriptions.Item>
              <Descriptions.Item label="排名">{p.ranked ? <Tag color="green">已排名</Tag> : <Tag color="orange">未排名</Tag>}</Descriptions.Item>
              {p.minWeightedAvg && <Descriptions.Item label="最低均分">≥ {p.minWeightedAvg}</Descriptions.Item>}
              {p.minPeScore && <Descriptions.Item label="最低体育">≥ {p.minPeScore}</Descriptions.Item>}
              <Descriptions.Item label="系统推荐等级" span={2}>
                {recommend ? <Tag color="gold" style={{ fontSize: 14 }}>{recommend.levelName} · {recommend.amount} 元</Tag>
                           : <span style={{ color: '#8c8c8c' }}>暂未生成推荐</span>}
              </Descriptions.Item>
              <Descriptions.Item label="说明" span={2}>{p.description || '—'}</Descriptions.Item>
            </Descriptions>
            <div style={{ marginTop: 12 }}>
              <span style={{ color: '#8c8c8c', marginRight: 8 }}>等级与比例：</span>
              {row.levels.map((l) => (
                <Tag key={l.id} color="blue">{l.levelName} {l.ratio}% · {l.amount} 元</Tag>
              ))}
            </div>
          </Card>
        )
      })}
    </Space>
  )
}
