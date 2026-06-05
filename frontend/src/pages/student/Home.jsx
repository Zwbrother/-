import { useEffect, useState } from 'react'
import { Card, Descriptions, Row, Col, Statistic, Tag, Button, Space, Spin } from 'antd'
import { useNavigate } from 'react-router-dom'
import api from '../../api.js'

export default function Home() {
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const navigate = useNavigate()

  const load = async () => {
    setLoading(true)
    try { setData(await api.get('/student/me')) } finally { setLoading(false) }
  }
  useEffect(() => { load() }, [])

  if (loading || !data) return <Spin />

  const stu = data.student
  const e = data.evaluation

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Card title="个人信息">
        <Descriptions column={2}>
          <Descriptions.Item label="学号">{stu.studentNo}</Descriptions.Item>
          <Descriptions.Item label="姓名">{stu.name}</Descriptions.Item>
          <Descriptions.Item label="学院">{stu.college}</Descriptions.Item>
          <Descriptions.Item label="专业">{stu.major}</Descriptions.Item>
          <Descriptions.Item label="年级">{stu.grade}</Descriptions.Item>
          <Descriptions.Item label="班级">{stu.className}</Descriptions.Item>
          <Descriptions.Item label="CET4">{stu.cet4Score > 0 ? stu.cet4Score : '—'}</Descriptions.Item>
          <Descriptions.Item label="CET6">{stu.cet6Score > 0 ? stu.cet6Score : '—'}</Descriptions.Item>
          <Descriptions.Item label="体育">{stu.peScore}</Descriptions.Item>
          <Descriptions.Item label="劳动教育">
            <Tag color={stu.laborEvaluation === 'PASS' ? 'green' : 'orange'}>{stu.laborEvaluation}</Tag>
          </Descriptions.Item>
        </Descriptions>
      </Card>

      {/* 基本项评分 */}
      <Card title={`当前学年（${data.year?.yearName || ''}）- 基本项测评`}
            extra={<Button type="primary" onClick={() => navigate('/student/basic-eval')}>填报基本项</Button>}>
        <Row gutter={16}>
          <Col span={4}><Statistic title="品德评议分" value={Number(e?.moralAppraisalScore || 0)} precision={2} /></Col>
          <Col span={4}><Statistic title="品德记实分" value={Number(e?.moralRecordScore || 0)} precision={2} /></Col>
          <Col span={4}><Statistic title="品德总分" value={Number(e?.moralTotal || 0)} precision={2} suffix="/ 100" /></Col>
          <Col span={4}><Statistic title="加权平均分" value={Number(e?.academicWeightedAvg || 0)} precision={2} /></Col>
          <Col span={4}>
            <Card type="inner" size="small" title="基本项总分" style={{ textAlign: 'center' }}>
              <div className="score-num">{Number(e?.basicTotal || 0).toFixed(2)}</div>
              <div style={{ fontSize: 12, color: '#8c8c8c' }}>基本项排名：{e?.basicRank ? `第 ${e.basicRank} 名` : '未排名'}</div>
            </Card>
          </Col>
        </Row>
        <div style={{ marginTop: 16, color: '#8c8c8c', fontSize: 12 }}>
          公式：基本项总分 = 品德总分 × 30% + 加权平均分 × 70%
        </div>
      </Card>

      {/* 综合能力评分 */}
      <Card title="综合能力测评"
            extra={<Button type="primary" onClick={() => navigate('/student/ability-eval')}>填报综合能力</Button>}>
        <Row gutter={16}>
          <Col span={4}><Statistic title="研究创新" value={Number(e?.researchInnovation || 0)} precision={2} suffix="×30%" /></Col>
          <Col span={4}><Statistic title="专业技能" value={Number(e?.professionalSkill || 0)} precision={2} suffix="×25%" /></Col>
          <Col span={4}><Statistic title="组织工作" value={Number(e?.organizationWork || 0)} precision={2} suffix="×15%" /></Col>
          <Col span={4}><Statistic title="体育美育" value={Number(e?.sportsAesthetics || 0)} precision={2} suffix="×15%" /></Col>
          <Col span={4}><Statistic title="劳动实践" value={Number(e?.laborPractice || 0)} precision={2} suffix="×15%" /></Col>
        </Row>
        <Row gutter={16} style={{ marginTop: 16 }}>
          <Col span={8}>
            <Card type="inner" size="small" title="综合能力总分" style={{ textAlign: 'center' }}>
              <div className="score-num">{Number(e?.abilityTotal || 0).toFixed(2)}</div>
              <div style={{ fontSize: 12, color: '#8c8c8c' }}>能力排名：{e?.abilityRank ? `第 ${e.abilityRank} 名` : '未排名'}</div>
            </Card>
          </Col>
          <Col span={8}>
            <Card type="inner" size="small" title="状态">
              <Tag color={e?.status === 'SUBMITTED' ? 'green' : 'gold'} style={{ fontSize: 14, padding: '4px 12px' }}>
                {e?.status === 'SUBMITTED' ? '已提交' : '草稿中'}
              </Tag>
            </Card>
          </Col>
          <Col span={8}>
            <Button block style={{ height: '100%' }} onClick={() => navigate('/student/scholarships')}>
              查看可申报奖学金
            </Button>
          </Col>
        </Row>
        <div style={{ marginTop: 16, color: '#8c8c8c', fontSize: 12 }}>
          公式：综合能力总分 = 75 + 研究创新×30% + 专业技能×25% + 组织工作×15% + 体育美育×15% + 劳动实践×15%
        </div>
      </Card>
    </Space>
  )
}
