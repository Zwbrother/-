import { useEffect, useState } from 'react'
import { Tabs, Table, Button, Space, Modal, Form, Input, Select, InputNumber, Tag, Popconfirm, message, Card, Alert, Upload, Image } from 'antd'
import { UploadOutlined, EditOutlined } from '@ant-design/icons'
import api from '../../api.js'

const STATUS_TAG = (s) => {
  const map = { PENDING: ['gold', '待审核'], APPROVED: ['green', '已通过'], REJECTED: ['red', '已驳回'] }
  const [color, text] = map[s] || ['default', s]
  return <Tag color={color}>{text}</Tag>
}

const DIMENSIONS = [
  { key: 'politicalLiteracy', label: '政治素养' },
  { key: 'legalAwareness', label: '法治观念' },
  { key: 'mentalQuality', label: '心理素质' },
  { key: 'integrityScore', label: '诚实守信' },
  { key: 'teamwork', label: '团队协作' },
  { key: 'socialResponsibility', label: '社会责任' }
]

export default function BasicEvaluation() {
  const [data, setData] = useState(null)
  const [modal, setModal] = useState({ visible: false, type: null, editRecord: null })
  const [form] = Form.useForm()
  const [uploading, setUploading] = useState(false)

  const load = async () => setData(await api.get('/student/evaluation/items'))
  useEffect(() => { load() }, [])

  const handleUpload = async ({ file, onSuccess, onError }) => {
    const fd = new FormData(); fd.append('file', file); setUploading(true)
    try {
      const res = await api.post('/student/upload', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
      onSuccess(res); return res.url
    } catch (e) { onError(e) } finally { setUploading(false) }
  }

  const save = async () => {
    const v = await form.validateFields()
    const fileList = v.attachmentUrl
    if (Array.isArray(fileList) && fileList.length > 0) {
      v.attachmentUrl = fileList[0].response?.url || fileList[0].url || null
    }

    if (modal.type === 'appraisal') {
      const key = modal.editRecord ? 'appraisals' : 'appraisals'
      if (modal.editRecord) {
        await api.put(`/student/evaluation/appraisals/${modal.editRecord.id}`, v)
      } else {
        await api.post('/student/evaluation/appraisals', v)
      }
    } else {
      const urls = { 'moral-record': '/student/evaluation/moral-records' }
      const baseUrl = urls[modal.type]
      if (modal.editRecord) {
        await api.put(`${baseUrl}/${modal.editRecord.id}`, v)
      } else {
        await api.post(baseUrl, v)
      }
    }
    message.success('已保存')
    setModal({ visible: false, type: null, editRecord: null })
    load()
  }

  const del = async (type, id) => {
    const base = type === 'appraisal' ? 'appraisals' : 'moral-records'
    await api.delete(`/student/evaluation/${base}/${id}`)
    message.success('已删除'); load()
  }

  const submit = async () => {
    await api.post('/student/evaluation/submit')
    message.success('已提交！'); load()
  }

  if (!data) return null
  const submitted = data.evaluation?.status === 'SUBMITTED'

  return (
    <Card title="基本项测评填报"
          extra={<Space>
            {submitted && <Tag color="green">已提交</Tag>}
            <Popconfirm title="确定提交？" onConfirm={submit}>
              <Button type="primary">{submitted ? '再次提交确认' : '提交测评'}</Button>
            </Popconfirm>
          </Space>}>
      <Alert type="info" style={{ marginBottom: 16 }} message="填报说明"
             description="基本项 = 品德总分 × 30% + 专业素质 × 70%。品德总分 = 评议分 × 70% + 记实分 × 30%。" showIcon />
      <Tabs defaultActiveKey="appraisal" items={[
        {
          key: 'appraisal',
          label: `品德评议 (${Number(data.evaluation?.moralAppraisalScore || 0).toFixed(1)})`,
          children: (
            <>
              <Button type="primary" style={{ marginBottom: 12 }} onClick={() => {
                form.resetFields(); setModal({ visible: true, type: 'appraisal', editRecord: null })
              }}>+ 新增评议</Button>
              <Table size="small" rowKey="id" dataSource={data.appraisals || []} pagination={false} scroll={{ x: 800 }}
                     columns={[
                       { title: '评议来源', dataIndex: 'appraiserType', render: (t) => ({SELF:'自评',STUDENT_REP:'学生代表',COUNSELOR:'辅导员'}[t]||t) },
                       ...DIMENSIONS.map(d => ({ title: d.label, dataIndex: d.key, render: v => Number(v||0) })),
                       { title: '合计', dataIndex: 'total', render: v => Number(v||0).toFixed(1) },
                       { title: '操作', render: (_, r) => (
                         <Space>
                           <Button size="small" icon={<EditOutlined />}
                                   onClick={() => { form.setFieldsValue(r); setModal({ visible: true, type: 'appraisal', editRecord: r }) }}>编辑</Button>
                           <Popconfirm title="删除？" onConfirm={() => del('appraisal', r.id)}>
                             <Button size="small" danger>删除</Button>
                           </Popconfirm>
                         </Space>
                       )}
                     ]} />
            </>
          )
        },
        {
          key: 'moral-record',
          label: `品德记实 (${Number(data.evaluation?.moralRecordScore || 0).toFixed(1)})`,
          children: (
            <>
              <Button type="primary" style={{ marginBottom: 12 }} onClick={() => {
                form.resetFields(); setModal({ visible: true, type: 'moral-record', editRecord: null })
              }}>+ 新增记实</Button>
              <Table size="small" rowKey="id" dataSource={data.moralRecords || []} pagination={false} scroll={{ x: 800 }}
                     columns={[
                       { title: '类型', dataIndex: 'itemType', render: t => ({VOLUNTEER:'志愿服务',DISCIPLINE:'处分扣分',HONOR:'荣誉',COLLECTIVE_HONOR:'集体荣誉'}[t]||t) },
                       { title: '说明', dataIndex: 'description' },
                       { title: '得分', dataIndex: 'score', render: v => Number(v||0).toFixed(2) },
                       { title: '审核', dataIndex: 'reviewStatus', render: STATUS_TAG },
                       { title: '驳回原因', dataIndex: 'reviewRemark', render: (v, r) => r.reviewStatus === 'REJECTED' ? <span style={{color:'red'}}>{v}</span> : null },
                       { title: '附件', dataIndex: 'attachmentUrl', render: url => url ? <Image src={url} width={40} /> : <Tag color="orange">无</Tag> },
                       { title: '操作', render: (_, r) => r.reviewStatus !== 'APPROVED' ? (
                         <Space>
                           <Button size="small" icon={<EditOutlined />} onClick={() => {
                             form.setFieldsValue({...r, attachmentUrl: r.attachmentUrl ? [{uid:'-1',status:'done',url:r.attachmentUrl}] : []})
                             setModal({ visible: true, type: 'moral-record', editRecord: r })
                           }}>{r.reviewStatus==='REJECTED'?'修改重审':'编辑'}</Button>
                           <Popconfirm title="删除？" onConfirm={() => del('moral-record', r.id)}>
                             <Button size="small" danger>删除</Button>
                           </Popconfirm>
                         </Space>
                       ) : null }
                     ]} />
            </>
          )
        },
        {
          key: 'courses',
          label: `专业素质 (${Number(data.evaluation?.academicWeightedAvg || 0).toFixed(2)})`,
          children: (
            <>
              <Alert type="info" style={{ marginBottom: 12 }} message="课程成绩由教务系统导入，学生不可修改。" />
              <Table size="small" rowKey="id" dataSource={data.courses || []} pagination={false}
                     columns={[{title:'课程',dataIndex:'courseName'},{title:'学分',dataIndex:'credit'},{title:'成绩',dataIndex:'score'}]} />
            </>
          )
        }
      ]} />

      <Modal open={modal.visible}
             title={modal.editRecord ? '修改记录' : '新增记录'}
             onCancel={() => setModal({ visible: false, type: null, editRecord: null })}
             onOk={save} okButtonProps={{ loading: uploading }} width={660} destroyOnClose>
        {modal.type === 'appraisal' && <AppraisalForm form={form} />}
        {modal.type === 'moral-record' && <MoralRecordForm form={form} handleUpload={handleUpload} />}
      </Modal>
    </Card>
  )
}

function AppraisalForm({ form }) {
  return (
    <Form form={form} layout="vertical" initialValues={{ appraiserType: 'SELF' }}>
      <Form.Item name="appraiserType" label="评议来源" rules={[{ required: true }]}>
        <Select options={[{value:'SELF',label:'自评'},{value:'STUDENT_REP',label:'学生代表'},{value:'COUNSELOR',label:'辅导员'}]} />
      </Form.Item>
      {DIMENSIONS.map(d => (
        <Form.Item key={d.key} name={d.key} label={d.label} rules={[{ required: true }]}
                   extra="0-20 分">
          <InputNumber min={0} max={20} step={0.5} style={{ width: '100%' }} />
        </Form.Item>
      ))}
    </Form>
  )
}

function MoralRecordForm({ form, handleUpload }) {
  const type = Form.useWatch('itemType', form)
  return (
    <Form form={form} layout="vertical" initialValues={{ itemType: 'VOLUNTEER' }}>
      <Form.Item name="itemType" label="类型" rules={[{ required: true }]}>
        <Select options={[
          {value:'VOLUNTEER',label:'志愿服务'},{value:'DISCIPLINE',label:'处分扣分'},
          {value:'HONOR',label:'荣誉表彰'},{value:'COLLECTIVE_HONOR',label:'集体荣誉'}
        ]} />
      </Form.Item>
      <Form.Item name="description" label="说明" rules={[{ required: true }]}>
        <Input.TextArea rows={2} placeholder="例：迎新志愿者 / 校三好学生" />
      </Form.Item>
      {type === 'VOLUNTEER' && (
        <Form.Item name="hours" label="志愿时长（小时）" rules={[{ required: true }]} extra="每4小时=1次(4分)，上限10分">
          <InputNumber min={0} style={{ width: '100%' }} />
        </Form.Item>
      )}
      {type === 'DISCIPLINE' && (
        <Form.Item name="rawValue" label="处分类型" rules={[{ required: true }]}>
          <Select options={[
            {value:1,label:'通报批评(-2)'},{value:2,label:'警告(-4)'},{value:3,label:'严重警告(-6)'},
            {value:4,label:'记过(-10)'},{value:5,label:'留校察看(-10)'},{value:6,label:'违法(-30)'}
          ]} />
        </Form.Item>
      )}
      <Form.Item name="attachmentUrl" label="证明材料" valuePropName="fileList"
                 getValueFromEvent={e => Array.isArray(e) ? e : e?.fileList}
                 rules={[{ required: true, message: '请上传证明材料' }]}>
        <Upload maxCount={1} accept="image/*,.pdf" customRequest={handleUpload} listType="picture">
          <Button icon={<UploadOutlined />}>点击上传</Button>
        </Upload>
      </Form.Item>
    </Form>
  )
}
