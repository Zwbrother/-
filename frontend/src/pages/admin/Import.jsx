import { useEffect, useState } from 'react'
import { Card, Row, Col, Button, Upload, Space, Alert, Table, Divider, Select, message, Typography } from 'antd'
import { UploadOutlined, DownloadOutlined } from '@ant-design/icons'
import { useAuthStore } from '../../store.js'
import api from '../../api.js'

const { Title, Text } = Typography

export default function Import() {
  const [years, setYears] = useState([])
  const [gradeYearId, setGradeYearId] = useState(null)
  const [studentResult, setStudentResult] = useState(null)
  const [gradeResult, setGradeResult] = useState(null)
  const [loading, setLoading] = useState({ students: false, grades: false })

  useEffect(() => {
    (async () => {
      const ys = await api.get('/admin/years')
      setYears(ys)
      const active = ys.find((y) => y.status === 'ACTIVE') || ys[0]
      if (active) setGradeYearId(active.id)
    })()
  }, [])

  const getToken = () => useAuthStore.getState().token || ''

  const authFetch = (url, filename) => {
    fetch(url, { headers: { Authorization: `Bearer ${getToken()}` } })
      .then((res) => res.blob())
      .then((blob) => {
        const a = document.createElement('a')
        a.href = URL.createObjectURL(blob)
        a.download = filename
        a.click()
        URL.revokeObjectURL(a.href)
      })
      .catch(() => message.error('下载失败'))
  }

  const downloadTemplate = (type) => {
    authFetch(
      `/api/admin/import/template/${type}`,
      type === 'students' ? '学生名单导入模板.xlsx' : '课程成绩导入模板.xlsx'
    )
  }


  const uploadStudents = async ({ file, onSuccess, onError }) => {
    setLoading((p) => ({ ...p, students: true }))
    const fd = new FormData()
    fd.append('file', file)
    try {
      const res = await api.post('/admin/import/students', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
      setStudentResult(res)
      message.success(`导入完成：成功 ${res.success} 条，跳过 ${res.skip} 条，错误 ${res.error} 条`)
      onSuccess(res)
    } catch (e) {
      onError(e)
    } finally {
      setLoading((p) => ({ ...p, students: false }))
    }
  }

  const uploadGrades = async ({ file, onSuccess, onError }) => {
    if (!gradeYearId) { message.warning('请先选择学年'); onError(new Error('no year')); return }
    setLoading((p) => ({ ...p, grades: true }))
    const fd = new FormData()
    fd.append('file', file)
    try {
      const res = await api.post(`/admin/import/grades?yearId=${gradeYearId}`, fd, { headers: { 'Content-Type': 'multipart/form-data' } })
      setGradeResult(res)
      message.success(`导入完成：成功 ${res.success} 条，跳过 ${res.skip} 条，错误 ${res.error} 条`)
      onSuccess(res)
    } catch (e) {
      onError(e)
    } finally {
      setLoading((p) => ({ ...p, grades: false }))
    }
  }

  const errorCols = [{ title: '错误信息', dataIndex: 'msg', render: (_, v) => v }]

  return (
    <div>
      <Title level={4} style={{ marginBottom: 24 }}>数据批量导入</Title>
      <Row gutter={24}>
        {/* 学生名单导入 */}
        <Col xs={24} lg={12}>
          <Card title="导入学生名单"
                extra={<Button size="small" icon={<DownloadOutlined />} onClick={() => downloadTemplate('students')}>下载模板</Button>}>
            <Alert
              type="info"
              showIcon
              style={{ marginBottom: 16 }}
              message="Excel 格式说明"
              description={
                <ul style={{ paddingLeft: 16, margin: 0 }}>
                  <li>必填列：学号、姓名、专业</li>
                  <li>专业填写：人工智能 / 通信工程 / 电子信息工程</li>
                  <li>培养类型：PROFESSIONAL（专业型）或 ACADEMIC（学术型）</li>
                  <li>学号即登录账号，初始密码与学号相同</li>
                  <li>已存在的学号会更新信息</li>
                </ul>
              }
            />
            <Upload
              accept=".xlsx,.xls"
              customRequest={uploadStudents}
              showUploadList={false}
              maxCount={1}
            >
              <Button type="primary" icon={<UploadOutlined />} loading={loading.students}>
                选择并上传学生名单
              </Button>
            </Upload>

            {studentResult && (
              <div style={{ marginTop: 16 }}>
                <Space>
                  <Text type="success">成功 {studentResult.success} 条</Text>
                  <Text type="warning">跳过 {studentResult.skip} 条</Text>
                  {studentResult.error > 0 && <Text type="danger">错误 {studentResult.error} 条</Text>}
                </Space>
                {studentResult.errors?.length > 0 && (
                  <Table
                    size="small"
                    style={{ marginTop: 8 }}
                    dataSource={studentResult.errors.map((e, i) => ({ key: i, msg: e }))}
                    columns={errorCols}
                    pagination={false}
                    scroll={{ y: 200 }}
                  />
                )}
              </div>
            )}
          </Card>
        </Col>

        {/* 课程成绩导入 */}
        <Col xs={24} lg={12}>
          <Card title="导入课程成绩 / GPA"
                extra={<Button size="small" icon={<DownloadOutlined />} onClick={() => downloadTemplate('grades')}>下载模板</Button>}>
            <Alert
              type="info"
              showIcon
              style={{ marginBottom: 16 }}
              message="Excel 格式说明"
              description={
                <ul style={{ paddingLeft: 16, margin: 0 }}>
                  <li>必填列：学号、课程名称、学分、成绩（0-100）</li>
                  <li>同一学年同课程已存在则覆盖</li>
                  <li>GPA 将根据成绩自动换算（≥90→4.0，80-89→3.0，etc.）</li>
                </ul>
              }
            />
            <Space style={{ marginBottom: 12 }}>
              <span>目标学年：</span>
              <Select
                style={{ width: 180 }}
                value={gradeYearId}
                onChange={setGradeYearId}
                options={years.map((y) => ({ value: y.id, label: y.yearName }))}
              />
            </Space>
            <div>
              <Upload
                accept=".xlsx,.xls"
                customRequest={uploadGrades}
                showUploadList={false}
                maxCount={1}
              >
                <Button type="primary" icon={<UploadOutlined />} loading={loading.grades}>
                  选择并上传成绩文件
                </Button>
              </Upload>
            </div>

            {gradeResult && (
              <div style={{ marginTop: 16 }}>
                <Space>
                  <Text type="success">成功 {gradeResult.success} 条</Text>
                  <Text type="warning">跳过 {gradeResult.skip} 条</Text>
                  {gradeResult.error > 0 && <Text type="danger">错误 {gradeResult.error} 条</Text>}
                </Space>
                {gradeResult.errors?.length > 0 && (
                  <Table
                    size="small"
                    style={{ marginTop: 8 }}
                    dataSource={gradeResult.errors.map((e, i) => ({ key: i, msg: e }))}
                    columns={errorCols}
                    pagination={false}
                    scroll={{ y: 200 }}
                  />
                )}
              </div>
            )}
          </Card>
        </Col>
      </Row>
    </div>
  )
}
