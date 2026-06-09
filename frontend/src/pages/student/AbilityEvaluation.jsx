import { useEffect, useState } from 'react'
import { Tabs, Table, Button, Space, Modal, Form, Input, Select, InputNumber, Tag, Popconfirm, message, Card, Alert, Upload, Image } from 'antd'
import { UploadOutlined, EditOutlined } from '@ant-design/icons'
import api from '../../api.js'

const STATUS_TAG = (s) => {
  const map = { PENDING: ['gold', '待审核'], APPROVED: ['green', '已通过'], REJECTED: ['red', '已驳回'] }
  const [color, text] = map[s] || ['default', s]
  return <Tag color={color}>{text}</Tag>
}

const COMMON_LEVEL = [
  { value: 'NATIONAL', label: '国家级' }, { value: 'PROVINCIAL', label: '省级' },
  { value: 'CITY', label: '市级' }, { value: 'SCHOOL', label: '校级' }, { value: 'COLLEGE', label: '院级' }
]
const COMMON_AWARD = [
  { value: 'FIRST', label: '一等' }, { value: 'SECOND', label: '二等' }, { value: 'THIRD', label: '三等' }, { value: 'PARTICIPATE', label: '参与' }
]
const LEVEL_MAP = { NATIONAL:'国家级', PROVINCIAL:'省级', CITY:'市级', SCHOOL:'校级', COLLEGE:'院级' }
const AWARD_MAP = { FIRST:'一等', SECOND:'二等', THIRD:'三等', PARTICIPATE:'参与' }

export default function AbilityEvaluation() {
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

  const getUrl = (type) => {
    const m = { ri:'ri-items', ps:'ps-items', ow:'ow-items', sa:'sa-items', lp:'lp-items' }
    return `/student/evaluation/${m[type]}`
  }

  const save = async () => {
    const v = await form.validateFields()
    const fl = v.attachmentUrl
    if (Array.isArray(fl) && fl.length > 0) v.attachmentUrl = fl[0].response?.url || fl[0].url || null
    const baseUrl = getUrl(modal.type)
    if (modal.editRecord) {
      await api.put(`${baseUrl}/${modal.editRecord.id}`, v)
      message.success('已修改')
    } else {
      await api.post(baseUrl, v)
      message.success('已添加')
    }
    setModal({ visible: false, type: null, editRecord: null })
    load()
  }

  const del = async (type, id) => {
    await api.delete(`/student/evaluation/${({ri:'ri',ps:'ps',ow:'ow',sa:'sa',lp:'lp'}[type])}-items/${id}`)
    message.success('已删除'); load()
  }

  const openAdd = (type) => { form.resetFields(); setModal({ visible: true, type, editRecord: null }) }
  const openEdit = (type, r) => {
    form.resetFields()
    form.setFieldsValue({...r, attachmentUrl: r.attachmentUrl ? [{uid:'-1',name:'附件',status:'done',url:r.attachmentUrl}] : []})
    setModal({ visible: true, type, editRecord: r })
  }

  const submit = async () => {
    await api.post('/student/evaluation/submit')
    message.success('已提交！'); load()
  }

  if (!data) return null
  const submitted = data.evaluation?.status === 'SUBMITTED'

  const tabItem = (key, label, items, columns, formComp) => ({
    key, label,
    children: (
      <>
        <Button type="primary" style={{ marginBottom: 12 }} onClick={() => openAdd(key)}>+ 新增</Button>
        <Table size="small" rowKey="id" dataSource={items} pagination={false} scroll={{ x: 800 }}
               columns={[
                 ...columns,
                 { title: '审核', dataIndex: 'reviewStatus', render: STATUS_TAG },
                 { title: '驳回', dataIndex: 'reviewRemark', render: (v,r) => r.reviewStatus==='REJECTED'?<span style={{color:'red'}}>{v||'—'}</span>:null },
                 { title: '附件', dataIndex: 'attachmentUrl', render: url => url?<Image src={url} width={40}/>:<Tag color="orange">无</Tag> },
                 { title: '操作', render: (_, r) => r.reviewStatus!=='APPROVED'?(
                   <Space>
                     <Button size="small" icon={<EditOutlined/>} onClick={()=>openEdit(key,r)}>
                       {r.reviewStatus==='REJECTED'?'修改重审':'编辑'}
                     </Button>
                     <Popconfirm title="删除？" onConfirm={()=>del(key,r.id)}><Button size="small" danger>删除</Button></Popconfirm>
                   </Space>
                 ):null }
               ]} />
      </>
    )
  })

  const items = [
    tabItem('ri', `研究创新 (${Number(data.evaluation?.researchInnovation||0).toFixed(1)})`, data.riItems||[], [
      {title:'类型',dataIndex:'itemType',render:t=>({COMPETITION:'学科竞赛',PAPER:'论文',PATENT:'专利',PROJECT:'科研项目'}[t]||t)},
      {title:'名称',dataIndex:'name'},{title:'级别',dataIndex:'levelField',render:v=>LEVEL_MAP[v]||v},{title:'奖项',dataIndex:'awardLevel',render:v=>AWARD_MAP[v]||v},
      {title:'得分',dataIndex:'score',render:v=>Number(v||0).toFixed(2)}
    ], <RIForm />),
    tabItem('ps', `专业技能 (${Number(data.evaluation?.professionalSkill||0).toFixed(1)})`, data.psItems||[], [
      {title:'类型',dataIndex:'itemType',render:t=>({CET4:'四级',CET6:'六级',COMPUTER:'计算机',CERTIFICATE:'资格证书',ENTRANCE_EXAM:'考研'}[t]||t)},
      {title:'名称',dataIndex:'name'},{title:'得分',dataIndex:'score',render:(v,r)=>{
        if (r.oralExamPassed && (r.itemType==='CET4'||r.itemType==='CET6')) {
          const base = Math.round(Number(v||0) - 2)
          return <span>{base}(<span style={{color:'#52c41a'}}>+2</span>)</span>
        }
        return Number(v||0).toFixed(2)
      }}
    ], <PSForm />),
    tabItem('ow', `组织工作 (${Number(data.evaluation?.organizationWork||0).toFixed(1)})`, data.owItems||[], [
      {title:'职务',dataIndex:'name'},{title:'岗位分',dataIndex:'positionScore'},
      {title:'绩效',dataIndex:'performanceGrade',render:t=>({EXCELLENT:'优秀',COMPETENT:'称职',INCOMPETENT:'不称职'}[t]||t)},
      {title:'任期(月)',dataIndex:'durationMonths'},{title:'得分',dataIndex:'score',render:v=>Number(v||0).toFixed(2)}
    ], <OWForm />),
    tabItem('sa', `体育美育 (${Number(data.evaluation?.sportsAesthetics||0).toFixed(1)})`, data.saItems||[], [
      {title:'类型',dataIndex:'itemType',render:t=>({SPORTS:'体育',AESTHETICS:'美育'}[t]||t)},
      {title:'名称',dataIndex:'name'},{title:'级别',dataIndex:'levelField',render:v=>LEVEL_MAP[v]||v},{title:'奖项',dataIndex:'awardLevel',render:v=>AWARD_MAP[v]||v},
      {title:'得分',dataIndex:'score',render:v=>Number(v||0).toFixed(2)}
    ], <SAForm />),
    tabItem('lp', `劳动实践 (${Number(data.evaluation?.laborPractice||0).toFixed(1)})`, data.lpItems||[], [
      {title:'类型',dataIndex:'itemType',render:t=>({LABOR:'劳动教育',SOCIAL_PRACTICE:'社会实践'}[t]||t)},
      {title:'名称',dataIndex:'name'},{title:'级别',dataIndex:'levelField',render:v=>LEVEL_MAP[v]||v},{title:'奖项',dataIndex:'awardLevel',render:v=>AWARD_MAP[v]||v},
      {title:'得分',dataIndex:'score',render:v=>Number(v||0).toFixed(2)}
    ], <SAForm />)
  ]

  return (
    <Card title="综合能力测评填报"
          extra={<Space>
            {submitted && <Tag color="green">已提交</Tag>}
            <Popconfirm title="确定提交？" onConfirm={submit}>
              <Button type="primary">{submitted ? '再次提交确认' : '提交测评'}</Button>
            </Popconfirm>
          </Space>}>
      <Alert type="info" style={{ marginBottom: 16 }} message="填报说明"
             description="综合能力 = 75 + 研究创新×30% + 专业技能×25% + 组织工作×15% + 体育美育×15% + 劳动实践×15%" showIcon />
      <Tabs defaultActiveKey="ri" items={items} />

      <Modal open={modal.visible}
             title={modal.editRecord ? '修改记录' : '新增记录'}
             onCancel={() => setModal({ visible: false, type: null, editRecord: null })}
             onOk={save} okButtonProps={{ loading: uploading }} width={660} destroyOnClose>
        {modal.type === 'ri' && <RIForm form={form} handleUpload={handleUpload} />}
        {modal.type === 'ps' && <PSForm form={form} handleUpload={handleUpload} />}
        {modal.type === 'ow' && <OWForm form={form} handleUpload={handleUpload} />}
        {modal.type === 'sa' && <SAForm form={form} handleUpload={handleUpload} />}
        {modal.type === 'lp' && <SAForm form={form} handleUpload={handleUpload} />}
      </Modal>
    </Card>
  )
}

function AttachmentField({ handleUpload }) {
  return (
    <Form.Item name="attachmentUrl" label="证明材料" valuePropName="fileList"
               getValueFromEvent={e => Array.isArray(e)?e:e?.fileList}
               rules={[{ required: true, message: '请上传' }]}>
      <Upload maxCount={1} accept="image/*,.pdf" customRequest={handleUpload} listType="picture">
        <Button icon={<UploadOutlined />}>点击上传</Button>
      </Upload>
    </Form.Item>
  )
}

function RIForm({ form, handleUpload }) {
  const type = Form.useWatch('itemType', form)
  return (
    <Form form={form} layout="vertical" initialValues={{ itemType: 'COMPETITION', hasAdvisor: false, totalAuthors: 1, myRank: 1 }}>
      <Form.Item name="itemType" label="类型" rules={[{ required: true }]}>
        <Select options={[{value:'COMPETITION',label:'学科竞赛'},{value:'PAPER',label:'论文'},{value:'PATENT',label:'专利'},{value:'PROJECT',label:'科研项目'}]} />
      </Form.Item>
      <Form.Item name="name" label="名称" rules={[{ required: true }]}><Input /></Form.Item>
      {type === 'COMPETITION' && <>
        <Form.Item name="competitionCategory" label="竞赛类别" rules={[{ required: true }]} extra="A×1.2, B×1.0, C×0.5">
          <Select options={[{value:'A',label:'A类'},{value:'B',label:'B类'},{value:'C',label:'C类'}]} />
        </Form.Item>
        <Form.Item name="levelField" label="级别" rules={[{ required: true }]}>
          <Select options={COMMON_LEVEL} />
        </Form.Item>
        <Form.Item name="awardLevel" label="奖项" rules={[{ required: true }]}>
          <Select options={COMMON_AWARD} />
        </Form.Item>
      </>}
      {type === 'PAPER' && (
        <Form.Item name="journalLevel" label="期刊级别" rules={[{ required: true }]}>
          <Select options={[{value:'FIRST_CLASS',label:'一级刊物(50)'},{value:'SECOND_CLASS',label:'二级刊物(25)'},{value:'THIRD_CLASS',label:'三级刊物(15)'}]} />
        </Form.Item>
      )}
      {type === 'PATENT' && (
        <Form.Item name="patentType" label="专利类型" rules={[{ required: true }]}>
          <Select options={[{value:'INVENTION',label:'发明专利(50)'},{value:'UTILITY',label:'实用新型(17)'},{value:'APPEARANCE',label:'外观设计(13)'}]} />
        </Form.Item>
      )}
      <Form.Item name="totalAuthors" label="合作总人数"><InputNumber min={1} style={{width:'100%'}} /></Form.Item>
      <Form.Item name="myRank" label="本人排名"><InputNumber min={1} style={{width:'100%'}} /></Form.Item>
      <Form.Item name="hasAdvisor" label="含导师"><Select options={[{value:false,label:'不含'},{value:true,label:'含'}]} /></Form.Item>
      <Form.Item name="isCoreMember" label="核心成员"><Select options={[{value:false,label:'否'},{value:true,label:'是(×0.8)'}]} /></Form.Item>
      <AttachmentField handleUpload={handleUpload} />
    </Form>
  )
}

function PSForm({ form, handleUpload }) {
  const type = Form.useWatch('itemType', form)
  return (
    <Form form={form} layout="vertical" initialValues={{ itemType: 'CET4', oralExamPassed: false, entranceExamResult: 'TOOK_EXAM' }}>
      <Form.Item name="itemType" label="类型" rules={[{ required: true }]}>
        <Select options={[{value:'CET4',label:'英语四级'},{value:'CET6',label:'英语六级'},{value:'COMPUTER',label:'计算机等级'},{value:'CERTIFICATE',label:'资格证书'},{value:'ENTRANCE_EXAM',label:'考研'}]} />
      </Form.Item>
      <Form.Item name="name" label="名称"><Input placeholder="例：CET-4 520分 / 计算机二级" /></Form.Item>
      {(type === 'CET4' || type === 'CET6') && (<>
        <Form.Item name="skillCategory" label="分数" extra="四级≥425 +6, ≥550 +10; 六级≥425 +12, ≥520 +15">
          <InputNumber min={0} max={710} style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item name="oralExamPassed" label="是否通过口语" extra="通过相应级别的口试者上浮2分">
          <Select options={[{value:false,label:'未通过'},{value:true,label:'已通过(+2)'}]} />
        </Form.Item>
      </>)}
      {type === 'COMPUTER' && (
        <Form.Item name="skillLevel" label="等级" extra="二级+6, 三级+10">
          <Select options={[{value:'LEVEL2',label:'二级'},{value:'LEVEL3',label:'三级'}]} />
        </Form.Item>
      )}
      {type === 'CERTIFICATE' && (
        <Form.Item name="skillLevel" label="级别" extra="高级30, 中级20, 初级10">
          <Select options={[{value:'HIGH',label:'高级'},{value:'MEDIUM',label:'中级'},{value:'PRIMARY',label:'初级'}]} />
        </Form.Item>
      )}
      {type === 'ENTRANCE_EXAM' && (
        <Form.Item name="entranceExamResult" label="考试结果" extra="参加并完成考试+12, 通过初试+16, 通过复试+20" rules={[{ required: true }]}>
          <Select options={[{value:'TOOK_EXAM',label:'参加并完成考试(+12)'},{value:'PASSED_INITIAL',label:'通过初试(+16)'},{value:'PASSED_REEXAM',label:'通过复试(+20)'}]} />
        </Form.Item>
      )}
      <AttachmentField handleUpload={handleUpload} />
    </Form>
  )
}

function OWForm({ form, handleUpload }) {
  return (
    <Form form={form} layout="vertical" initialValues={{ durationMonths: 12, performanceGrade: 'EXCELLENT' }}>
      <Alert type="info" style={{ marginBottom: 12 }} message="计分规则：任多项职务者以最高分职务计分；任职≥12个月全额，6-11个月折半，不足6个月不计分" showIcon />
      <Form.Item name="name" label="职务名称" rules={[{ required: true }]}><Input placeholder="班长 / 学生会主席" /></Form.Item>
      <Form.Item name="orgLevel" label="层级">
        <Select options={[{value:'SCHOOL',label:'校级'},{value:'COLLEGE',label:'院级'},{value:'CLASS',label:'班级'},{value:'CLUB',label:'社团'}]} />
      </Form.Item>
      <Form.Item name="positionScore" label="岗位分" rules={[{ required: true }]} extra="9~18分">
        <InputNumber min={9} max={18} style={{ width: '100%' }} />
      </Form.Item>
      <Form.Item name="performanceGrade" label="考核" rules={[{ required: true }]}>
        <Select options={[{value:'EXCELLENT',label:'优秀(+8)'},{value:'COMPETENT',label:'称职(+2)'},{value:'INCOMPETENT',label:'不称职(0)'}]} />
      </Form.Item>
      <Form.Item name="durationMonths" label="任期(月)" extra="<6不计, 6-11折半">
        <InputNumber min={1} max={48} style={{ width: '100%' }} />
      </Form.Item>
      <AttachmentField handleUpload={handleUpload} />
    </Form>
  )
}

function SAForm({ form, handleUpload }) {
  return (
    <Form form={form} layout="vertical" initialValues={{ isTeam: false, isCoreMember: false }}>
      <Form.Item name="itemType" label="类型" rules={[{ required: true }]}>
        <Select options={[{value:'SPORTS',label:'体育'},{value:'AESTHETICS',label:'美育'},{value:'LABOR',label:'劳动教育'},{value:'SOCIAL_PRACTICE',label:'社会实践'}]} />
      </Form.Item>
      <Form.Item name="name" label="名称" rules={[{ required: true }]}><Input /></Form.Item>
      <Form.Item name="levelField" label="级别" rules={[{ required: true }]}>
        <Select options={COMMON_LEVEL} />
      </Form.Item>
      <Form.Item name="awardLevel" label="奖项" rules={[{ required: true }]}>
        <Select options={COMMON_AWARD} />
      </Form.Item>
      <Form.Item name="isTeam" label="团体项目"><Select options={[{value:false,label:'否'},{value:true,label:'是'}]} /></Form.Item>
      <Form.Item name="isCoreMember" label="核心成员"><Select options={[{value:false,label:'否'},{value:true,label:'是(×0.8)'}]} /></Form.Item>
      <AttachmentField handleUpload={handleUpload} />
    </Form>
  )
}
