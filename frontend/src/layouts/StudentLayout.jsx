import { HomeOutlined, FormOutlined, ThunderboltOutlined, GiftOutlined, FileTextOutlined } from '@ant-design/icons'
import BaseLayout from './BaseLayout.jsx'

const items = [
  { key: '/student', icon: <HomeOutlined />, label: '个人主页' },
  { key: '/student/basic-eval', icon: <FormOutlined />, label: '基本项测评' },
  { key: '/student/ability-eval', icon: <ThunderboltOutlined />, label: '综合能力测评' },
  { key: '/student/scholarships', icon: <GiftOutlined />, label: '奖学金申报' },
  { key: '/student/applications', icon: <FileTextOutlined />, label: '我的申请' }
]

export default function StudentLayout() {
  return <BaseLayout title="学生端" basePath="/student" menuItems={items} />
}
