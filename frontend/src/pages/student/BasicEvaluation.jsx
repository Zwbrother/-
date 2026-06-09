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
                       { title: '合计', render: (_, r) => {
                         const sum = DIMENSIONS.reduce((s, d) => s + Number(r[d.key] || 0), 0)
                         return <strong>{sum.toFixed(1)}</strong>
                       }},
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
                       { title: '类型', dataIndex: 'itemType', sorter: (a, b) => {
                         const order = { HONOR: 1, COLLECTIVE_HONOR: 2, DISCIPLINE: 3, VOLUNTEER: 4 }
                         return (order[a.itemType] || 99) - (order[b.itemType] || 99)
                       }, render: t =>
                         ({VOLUNTEER:'志愿服务',DISCIPLINE:'处分扣分',HONOR:'荣誉',COLLECTIVE_HONOR:'集体荣誉'}[t]||t)
                       },
                       { title: '等级/详情', render: (_, r) => {
                         const HONOR_MAP = {NATIONAL:{label:'国家级',s:20},PROVINCIAL:{label:'省级',s:15},CITY:{label:'市级',s:12},SCHOOL:{label:'校级',s:8},COLLEGE:{label:'院级',s:5},CLASS:{label:'班级',s:2}}
                         const CH_MAP = {EXCELLENT_STUDY_STYLE:{label:'学风优良班',s:5},SPECIAL_STUDY_STYLE:{label:'学风特优班',s:10},ADVANCED_LEAGUE:{label:'先进团支部',s:5},MAY4TH_LEAGUE:{label:'五四团支部',s:8}}
                         // 按分值反查标签（用于 honorLevel 为空的旧数据兼容）
                         const guessByScore = (map, s) => Object.values(map).find(x => x.s === s)
                         // 获取delta：rawValue > score(已计算) > map默认值
                         const getDelta = (map, key) => {
                           const raw = Number(r.rawValue)
                           if (!isNaN(raw) && raw > 0) return raw
                           const s = Number(r.score)
                           if (!isNaN(s) && s > 0) return s
                           const entry = map[key]
                           return entry ? entry.s : 0
                         }
                         const getDiscDelta = () => {
                           const raw = Number(r.rawValue)
                           return !isNaN(raw) && raw > 0 ? -raw : -(Number(r.rawValue) || 0)
                         }
                         const fmt = (label, delta) => {
                           const color = delta > 0 ? '#52c41a' : delta < 0 ? '#ff4d4f' : '#666'
                           const sign = delta > 0 ? '+' : ''
                           const d = delta.toFixed(1).replace(/\.0$/,'')
                           return <span>{label} <span style={{color,fontWeight:'bold'}}>({sign}{d})</span></span>
                         }
                         if (r.itemType === 'HONOR') {
                           const delta = getDelta(HONOR_MAP, r.honorLevel)
                           const entry = HONOR_MAP[r.honorLevel] || guessByScore(HONOR_MAP, delta)
                           const label = entry ? entry.label : '荣誉'
                           return fmt(label, delta)
                         }
                         if (r.itemType === 'DISCIPLINE') {
                           const delta = getDiscDelta()
                           // honorLevel 存储中文处分类型（如"警告"），为空时兜底"处分"
                           const label = r.honorLevel || '处分'
                           return fmt(label, delta)
                         }
                         if (r.itemType === 'COLLECTIVE_HONOR') {
                           const delta = getDelta(CH_MAP, r.honorLevel)
                           const entry = CH_MAP[r.honorLevel] || guessByScore(CH_MAP, delta)
                           const label = entry ? entry.label : '集体荣誉'
                           return fmt(label, delta)
                         }
                         if (r.itemType === 'VOLUNTEER') {
                           const h = Number(r.hours) || 0
                           const delta = Math.min(Math.floor(h/4)*4 + (h%4>0?2:0), 10)
                           return fmt(`${h}h`, delta)
                         }
                         return null
                       }},
                       { title: '说明', dataIndex: 'description' },
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
  const honorLevel = Form.useWatch('honorLevel', form)
  const rawValue = Form.useWatch('rawValue', form)

  // 统一配置：{ value, label, score (固定分值) }
  // 荣誉表彰为固定分值（国家级20/省级15/市级12/校级8/院级5/班级2）
  // 院级和班级加分累计上限20分，校级及以上不设限
  const HONOR_LEVELS = [
    { value: 'NATIONAL', label: '国家级', score: 20 },
    { value: 'PROVINCIAL', label: '省级', score: 15 },
    { value: 'CITY', label: '市级', score: 12 },
    { value: 'SCHOOL', label: '校级', score: 8 },
    { value: 'COLLEGE', label: '院级', score: 5 },
    { value: 'CLASS', label: '班级', score: 2 },
  ]

  // 处分为范围值（min/max），允许在范围内调整
  const DISCIPLINE_TYPES = [
    { value: '通报批评', label: '通报批评', min: 0.5, max: 2, def: 2 },
    { value: '警告', label: '警告', min: 2, max: 4, def: 4 },
    { value: '严重警告', label: '严重警告', min: 4, max: 6, def: 6 },
    { value: '记过', label: '记过', min: 8, max: 10, def: 10 },
    { value: '留校察看', label: '留校察看', min: 8, max: 10, def: 10 },
    { value: '违法', label: '违法', min: 10, max: 30, def: 30 },
  ]

  // 集体荣誉为固定分值（学风优良班5/学风特优班10/先进团支部5/五四团支部8）
  const COLLECTIVE_HONOR_TYPES = [
    { value: 'EXCELLENT_STUDY_STYLE', label: '学风优良班', score: 5 },
    { value: 'SPECIAL_STUDY_STYLE', label: '学风特优班', score: 10 },
    { value: 'ADVANCED_LEAGUE', label: '先进团支部', score: 5 },
    { value: 'MAY4TH_LEAGUE', label: '五四团支部', score: 8 },
  ]

  // 根据当前 type 获取对应的配置列表
  const getLevelList = () => {
    if (type === 'HONOR') return HONOR_LEVELS
    if (type === 'DISCIPLINE') return DISCIPLINE_TYPES
    if (type === 'COLLECTIVE_HONOR') return COLLECTIVE_HONOR_TYPES
    return []
  }

  // 查找当前选中项的配置
  const getLevelInfo = () => {
    const list = getLevelList()
    return list.find(item => item.value === honorLevel) || null
  }

  // 是否有范围（处分是可调的，荣誉是固定的）
  const isRanged = () => {
    const info = getLevelInfo()
    return info && info.min !== undefined && info.max !== undefined
  }

  // 获取当前显示分值（固定分值或范围内当前值）
  const getDisplayScore = () => {
    const info = getLevelInfo()
    if (!info) return 0
    // 固定分值
    if (info.score !== undefined) return info.score
    // 范围值
    return rawValue != null ? rawValue : info.def
  }

  // 加减分（仅处分范围可用，0.5步长）
  const adjustScore = (delta) => {
    const info = getLevelInfo()
    if (!info || info.min === undefined) return
    const current = rawValue != null ? rawValue : info.def
    const next = Math.round((current + delta) * 10) / 10
    if (next >= info.min && next <= info.max) {
      form.setFieldsValue({ rawValue: next })
    }
  }

  // 切换等级/类型时重置分值
  const handleLevelChange = (value) => {
    const list = getLevelList()
    const info = list.find(item => item.value === value)
    if (info) {
      if (info.score !== undefined) {
        form.setFieldsValue({ honorLevel: value, rawValue: info.score })
      } else {
        form.setFieldsValue({ honorLevel: value, rawValue: info.def })
      }
    }
  }

  // 标签文字：荣誉→"加分"，处分→"扣分"，集体荣誉→"加分"
  const scoreLabel = type === 'DISCIPLINE' ? '扣分' : '加分'

  return (
    <Form form={form} layout="vertical" initialValues={{ itemType: 'HONOR' }}>
      <Form.Item name="itemType" label="类型" rules={[{ required: true }]}>
        <Select onChange={() => {
          form.setFieldsValue({ honorLevel: undefined, rawValue: undefined })
        }} options={[
          {value:'HONOR',label:'荣誉表彰'},{value:'DISCIPLINE',label:'处分扣分'},
          {value:'COLLECTIVE_HONOR',label:'集体荣誉'}
        ]} />
      </Form.Item>

      {/* ===== 荣誉表彰 / 处分扣分 / 集体荣誉（统一布局） ===== */}
      {(type === 'HONOR' || type === 'DISCIPLINE' || type === 'COLLECTIVE_HONOR') && (
        <>
          <Form.Item name="honorLevel" label={type==='DISCIPLINE'?'处分类型':type==='HONOR'?'荣誉等级':'集体荣誉类型'}
                     rules={[{ required: true, message: '请选择' }]}>
            <Select onChange={handleLevelChange} placeholder="请选择" options={
              getLevelList().map(item => ({
                value: item.value,
                label: item.score !== undefined
                  ? `${item.label} (${type==='DISCIPLINE'?'扣':'+'}${item.score}分)`
                  : `${item.label} (${type==='DISCIPLINE'?'扣':'+'}${item.min}~${item.max}分)`
              }))
            } />
          </Form.Item>
          {honorLevel && (() => {
            const info = getLevelInfo()
            if (!info) return null
            const ranged = isRanged()
            const currentScore = getDisplayScore()
            return (
              <Form.Item label={scoreLabel} required>
                <Space size="middle" align="center">
                  {ranged && (
                    <Button onClick={() => adjustScore(-0.5)}
                            disabled={currentScore <= info.min}>
                      -0.5
                    </Button>
                  )}
                  {ranged ? (
                    <InputNumber min={info.min} max={info.max} step={0.5}
                                 value={currentScore}
                                 style={{ width: 100, textAlign: 'center' }}
                                 readOnly />
                  ) : (
                    <span style={{ fontSize: 18, fontWeight: 'bold', color: '#1677ff' }}>
                      {type==='DISCIPLINE'?'-':'+'}{currentScore} 分
                    </span>
                  )}
                  {ranged && (
                    <Button onClick={() => adjustScore(0.5)}
                            disabled={currentScore >= info.max}>
                      +0.5
                    </Button>
                  )}
                  <span style={{ color: '#888', fontSize: 12, marginLeft: 8 }}>
                    {ranged
                      ? `（范围：${info.min} ~ ${info.max}，步长 0.5）`
                      : '（固定分值）'}
                  </span>
                </Space>
              </Form.Item>
            )
          })()}
          <Form.Item name="description" label="说明" rules={[{ required: true }]}>
            <Input placeholder={type==='DISCIPLINE'?'请说明处分原因':type==='HONOR'?'例：省级三好学生 / 国家奖学金':'例：2024-2025学年学风优良班'} />
          </Form.Item>
        </>
      )}

      {/* ===== 上传附件 ===== */}
      <Form.Item name="attachmentUrl" label="证明材料" valuePropName="fileList"
                 getValueFromEvent={e => Array.isArray(e) ? e : e?.fileList}
                 rules={[{ required: true, message: '请上传证明材料' }]}>
        <Upload maxCount={1} accept="image/*,.pdf" customRequest={handleUpload} listType="picture">
          <Button icon={<UploadOutlined />}>点击上传</Button>
        </Upload>
      </Form.Item>

      {/* 隐藏字段：rawValue 存储实际分值 */}
      <Form.Item name="rawValue" hidden>
        <InputNumber />
      </Form.Item>
    </Form>
  )
}
