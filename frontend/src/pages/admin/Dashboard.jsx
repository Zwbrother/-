import { useEffect, useState } from 'react'
import { Row, Col, Card, Statistic, Spin, Alert } from 'antd'
import { TeamOutlined, BookOutlined, FileTextOutlined, CheckCircleOutlined, ClockCircleOutlined } from '@ant-design/icons'
import api from '../../api.js'

export default function Dashboard() {
  const [stats, setStats] = useState(null)

  useEffect(() => { (async () => setStats(await api.get('/admin/stats/dashboard')))() }, [])

  if (!stats) return <Spin />

  return (
    <>
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
        message="操作指南"
        description="1️⃣ 学年管理 → 创建/选择当前学年；2️⃣ 奖学金项目 → 创建学业奖学金项目并设置一/二/三等比例；3️⃣ 学生填报综测，辅导员审核；4️⃣ 项目页一键「执行排名」自动按比例分配等级；5️⃣ 辅导员审核申请后，管理员点击「发布公示」。"
      />
      <Row gutter={16}>
        <Col span={6}><Card><Statistic title="学生总数" value={stats.students} prefix={<TeamOutlined />} /></Card></Col>
        <Col span={6}><Card><Statistic title="奖学金项目" value={stats.projects} prefix={<BookOutlined />} /></Card></Col>
        <Col span={6}><Card><Statistic title="申请总数" value={stats.applicationsTotal} prefix={<FileTextOutlined />} /></Card></Col>
        <Col span={6}><Card><Statistic title="待审申请" value={stats.applicationsPending} prefix={<ClockCircleOutlined />} valueStyle={{ color: '#faad14' }} /></Card></Col>
      </Row>
      <Row gutter={16} style={{ marginTop: 16 }}>
        <Col span={6}><Card><Statistic title="已通过/公示" value={stats.applicationsApproved} prefix={<CheckCircleOutlined />} valueStyle={{ color: '#52c41a' }} /></Card></Col>
        <Col span={6}><Card><Statistic title="综测记录" value={stats.evaluations} /></Card></Col>
      </Row>
    </>
  )
}
