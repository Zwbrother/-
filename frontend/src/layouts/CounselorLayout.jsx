import { TeamOutlined, AuditOutlined, CheckSquareOutlined } from '@ant-design/icons'
import BaseLayout from './BaseLayout.jsx'

const items = [
  { key: '/counselor', icon: <TeamOutlined />, label: '我的学生' },
  { key: '/counselor/review', icon: <AuditOutlined />, label: '材料审核' },
  { key: '/counselor/applications', icon: <CheckSquareOutlined />, label: '申请审核' }
]

export default function CounselorLayout() {
  return <BaseLayout title="辅导员端" basePath="/counselor" menuItems={items} />
}
