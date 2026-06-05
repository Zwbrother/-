import { DashboardOutlined, CalendarOutlined, BookOutlined, TrophyOutlined, ImportOutlined } from '@ant-design/icons'
import BaseLayout from './BaseLayout.jsx'

const items = [
  { key: '/admin', icon: <DashboardOutlined />, label: '统计看板' },
  { key: '/admin/years', icon: <CalendarOutlined />, label: '学年管理' },
  { key: '/admin/projects', icon: <BookOutlined />, label: '奖学金项目' },
  { key: '/admin/ranking', icon: <TrophyOutlined />, label: '综测排名' },
  { key: '/admin/import', icon: <ImportOutlined />, label: '数据导入' }
]

export default function AdminLayout() {
  return <BaseLayout title="管理员端" basePath="/admin" menuItems={items} />
}
