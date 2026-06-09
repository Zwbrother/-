import { useState, useEffect } from 'react'
import { Card, Form, Input, Radio, Button, message, Descriptions, Tag, Alert, Upload, Image } from 'antd'
import { SendOutlined, UploadOutlined } from '@ant-design/icons'
import api from '../../api.js'

export default function GraduateExam() {
  const [form] = Form.useForm()
  const [existing, setExisting] = useState(null)
  const [loading, setLoading] = useState(false)
  const [me, setMe] = useState(null)
  const [submitting, setSubmitting] = useState(false)
  const [uploading, setUploading] = useState(false)

  const load = async () => {
    setLoading(true)
    try {
      const d = await api.get('/student/graduate-exam')
      setExisting(d?.application)
      setMe(d?.student)
    } finally { setLoading(false) }
  }

  useEffect(() => { load() }, [])

  const handleUpload = async ({ file, onSuccess, onError }) => {
    const fd = new FormData(); fd.append('file', file); setUploading(true)
    try {
      const res = await api.post('/student/upload', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
      onSuccess(res); return res.url
    } catch (e) { onError(e) } finally { setUploading(false) }
  }

  const handleSubmit = async (values) => {
    setSubmitting(true)
    try {
      const fl = values.attachmentUrl
      if (Array.isArray(fl) && fl.length > 0) values.attachmentUrl = fl[0].response?.url || fl[0].url || null
      await api.post('/student/graduate-exam', values)
      message.success('考研奖学金申报已提交')
      load()
    } finally { setSubmitting(false) }
  }

  const LEVEL_INFO = { FIRST: { color: 'gold', label: '一等奖 600元', desc: '已录取为硕士研究生（含国外）' },
                        SECOND: { color: 'blue', label: '二等奖 300元', desc: '获得复试资格' } }

  if (existing && existing.status === 'SUBMITTED') {
    const info = LEVEL_INFO[existing.finalLevel]
    return (
      <Card title="考研奖学金申报状态">
        <Alert type="info" message="您的申报已提交，请等待辅导员审核" style={{ marginBottom: 16 }} />
        <Descriptions column={2} bordered size="small">
          <Descriptions.Item label="考试类型">{existing.examType === 'DOMESTIC' ? '国内考研' : '国外申研'}</Descriptions.Item>
          <Descriptions.Item label="报考学校">{existing.schoolName || '-'}</Descriptions.Item>
          <Descriptions.Item label="报考专业">{existing.majorName || '-'}</Descriptions.Item>
          <Descriptions.Item label="复试资格">{existing.hasInterviewQualification ? '✅ 已获得' : '❌ 未获得'}</Descriptions.Item>
          <Descriptions.Item label="录取状态">{existing.isAdmitted ? '✅ 已录取' : '❌ 未录取'}</Descriptions.Item>
          <Descriptions.Item label="系统判定等级">
            {info ? <Tag color={info.color}>{info.label} · {info.desc}</Tag> : <Tag>未达到获奖条件</Tag>}
          </Descriptions.Item>
          <Descriptions.Item label="审核状态"><Tag color='gold'>待审核</Tag></Descriptions.Item>
          <Descriptions.Item label="证明材料">
            {existing.attachmentUrl ? <Image src={existing.attachmentUrl} width={120} /> : '—'}
          </Descriptions.Item>
        </Descriptions>
      </Card>
    )
  }

  if (existing && existing.status === 'APPROVED') {
    const info = LEVEL_INFO[existing.finalLevel]
    return (
      <Card title="考研奖学金申报状态">
        <Alert type="success" message="您的考研奖学金申报已通过审核" style={{ marginBottom: 16 }} />
        <Descriptions column={2} bordered size="small">
          <Descriptions.Item label="考试类型">{existing.examType === 'DOMESTIC' ? '国内考研' : '国外申研'}</Descriptions.Item>
          <Descriptions.Item label="报考学校">{existing.schoolName || '-'}</Descriptions.Item>
          <Descriptions.Item label="报考专业">{existing.majorName || '-'}</Descriptions.Item>
          <Descriptions.Item label="复试资格">{existing.hasInterviewQualification ? '✅ 已获得' : '❌ 未获得'}</Descriptions.Item>
          <Descriptions.Item label="录取状态">{existing.isAdmitted ? '✅ 已录取' : '❌ 未录取'}</Descriptions.Item>
          <Descriptions.Item label="最终等级">
            {info ? <Tag color={info.color}>{info.label} · {info.desc}</Tag> : <Tag>未达到获奖条件</Tag>}
          </Descriptions.Item>
          <Descriptions.Item label="审核状态"><Tag color='green'>已通过</Tag></Descriptions.Item>
          <Descriptions.Item label="证明材料">
            {existing.attachmentUrl ? <Image src={existing.attachmentUrl} width={120} /> : '—'}
          </Descriptions.Item>
        </Descriptions>
      </Card>
    )
  }

  const rejected = existing && (existing.status === 'REJECTED' || existing.status === 'WITHDRAWN')
  const resubmit = !!existing

  return (
    <Card title={resubmit ? '考研奖学金申报（重新提交）' : '考研奖学金申报'} loading={loading}>
      {rejected && <Alert type="warning" message="您的申报已被退回，请修改后重新提交"
        description={existing.rejectReason ? `退回原因：${existing.rejectReason}` : undefined}
        style={{ marginBottom: 16 }} showIcon />}
      {!rejected && !resubmit && <Alert type="info" message="考研奖学金独立于综合素质评价专业技能模块，每人每学年限申报一次。"
        style={{ marginBottom: 16 }} />}
      <Form form={form} layout="vertical" onFinish={handleSubmit}
        initialValues={resubmit ? {
          examType: existing.examType || 'DOMESTIC',
          schoolName: existing.schoolName,
          majorName: existing.majorName,
          hasInterviewQualification: existing.hasInterviewQualification || false,
          isAdmitted: existing.isAdmitted || false,
          attachmentUrl: existing.attachmentUrl ? [{uid:'-1',name:'附件',status:'done',url:existing.attachmentUrl}] : []
        } : { examType: 'DOMESTIC', hasInterviewQualification: false, isAdmitted: false }}>
        <Form.Item label="考试类型" name="examType" rules={[{ required: true }]}>
          <Radio.Group>
            <Radio.Button value="DOMESTIC">国内考研</Radio.Button>
            <Radio.Button value="OVERSEAS">国外申研</Radio.Button>
          </Radio.Group>
        </Form.Item>
        <Form.Item label="报考学校" name="schoolName" rules={[{ required: true, message: '请填写报考学校' }]}>
          <Input placeholder="如：浙江大学" />
        </Form.Item>
        <Form.Item label="报考专业" name="majorName">
          <Input placeholder="如：计算机科学与技术" />
        </Form.Item>
        <Form.Item label="是否获得复试资格" name="hasInterviewQualification" rules={[{ required: true, message: '请选择复试资格' }]}>
          <Radio.Group>
            <Radio value={true}>是（二等奖300元）</Radio>
            <Radio value={false}>否</Radio>
          </Radio.Group>
        </Form.Item>
        <Form.Item label="是否已被录取" name="isAdmitted"
          rules={[
            { required: true, message: '请选择录取状态' },
            ({ getFieldValue }) => ({
              validator(_, value) {
                if (value === true && getFieldValue('hasInterviewQualification') !== true) {
                  return Promise.reject(new Error('已录取必须同时具有复试资格'))
                }
                return Promise.resolve()
              }
            })
          ]}>
          <Radio.Group>
            <Radio value={true}>是（一等奖600元）</Radio>
            <Radio value={false}>否</Radio>
          </Radio.Group>
        </Form.Item>
        <Form.Item name="attachmentUrl" label="证明材料（录取/复试截图等）" valuePropName="fileList"
                   getValueFromEvent={e => Array.isArray(e)?e:e?.fileList}
                   rules={[{ required: true, message: '请上传证明材料' }]}>
          <Upload maxCount={1} accept="image/*,.pdf" customRequest={handleUpload} listType="picture">
            <Button icon={<UploadOutlined />}>点击上传</Button>
          </Upload>
        </Form.Item>
        <Form.Item>
          <Button type="primary" htmlType="submit" icon={<SendOutlined />} loading={submitting || uploading}>
            {resubmit ? '重新提交' : '提交申报'}
          </Button>
        </Form.Item>
      </Form>
    </Card>
  )
}
