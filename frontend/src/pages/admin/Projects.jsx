import { useEffect, useState } from 'react'
import { Card, Button, Table, Tag, Modal, Form, Input, InputNumber, Select, Space, message,
         Popconfirm, Descriptions, Divider, Tabs, Badge, Statistic, Row, Col } from 'antd'
import { PlusOutlined, DeleteOutlined, TrophyOutlined } from '@ant-design/icons'
import api from '../../api.js'

const LEVEL_COLORS = ['gold', 'silver', '#cd7f32', 'default', 'blue', 'cyan', 'purple']
const LEVEL_ICONS = ['🥇', '🥈', '🥉', '🎖️']

const SCHOLARSHIP_TYPES = [
  { value: 'COMPREHENSIVE', label: '优秀学生综合奖学金（一/二/三等）' },
  { value: 'ABILITY', label: '能力突出奖学金' },
  { value: 'GRADUATE_EXAM', label: '考研奖学金' },
  { value: 'SPECIAL', label: '单项奖学金' },
  { value: 'NATIONAL', label: '国家奖学金' },
  { value: 'PROVINCIAL', label: '省政府奖学金' },
  { value: 'NAMED', label: '专项奖学金' }
]

export default function Projects() {
  const [years, setYears] = useState([])
  const [list, setList] = useState([])
  const [visible, setVisible] = useState(false)
  const [editingId, setEditingId] = useState(null) // null = create, number = edit
  const [awardModal, setAwardModal] = useState({ open: false, data: null, projectName: '' })
  const [form] = Form.useForm()

  const load = async () => {
    const [ys, ps] = await Promise.all([api.get('/admin/years'), api.get('/admin/projects')])
    setYears(ys)
    setList(ps)
  }
  useEffect(() => { load() }, [])

  const save = async () => {
    const v = await form.validateFields()
    // 根据 allocType 清理多余字段，保证后端只收到一种分配方式
    if (v.levels) {
      v.levels = v.levels.map(({ allocType, ...l }) => ({
        ...l,
        ratio: allocType === 'ratio' ? l.ratio : null,
        quota: allocType === 'fixed' ? l.quota : null
      }))
    }
    if (editingId) {
      await api.put(`/admin/projects/${editingId}`, v)
      message.success('已保存修改')
    } else {
      await api.post('/admin/projects', v)
      message.success('已创建奖学金项目')
    }
    setVisible(false)
    setEditingId(null)
    form.resetFields()
    load()
  }

  const openEdit = (row) => {
    form.resetFields()
    form.setFieldsValue({
      academicYearId: row.project.academicYearId,
      typeCode: row.project.typeCode,
      projectName: row.project.projectName,
      description: row.project.description,
      levels: row.levels.map(l => ({
        levelName: l.levelName,
        levelOrder: l.levelOrder,
        allocType: l.quota != null ? 'fixed' : 'ratio',
        ratio: l.ratio != null ? Number(l.ratio) : null,
        quota: l.quota != null ? Number(l.quota) : null,
        amount: l.amount != null ? Number(l.amount) : null
      })),
      criteria: row.criteria.map(c => ({ ruleType: c.ruleType, ruleValue: c.ruleValue }))
    })
    setEditingId(row.project.id)
    setVisible(true)
  }

  const triggerRank = async (id, projectName) => {
    await api.post(`/admin/projects/${id}/rank`)
    message.success('排名与等级分配已完成')
    load()
    // 自动拉取获奖名单预览
    showAwardPreview(id, projectName)
  }

  const showAwardPreview = async (id, projectName) => {
    const data = await api.get(`/admin/projects/${id}/award-preview`)
    setAwardModal({ open: true, data, projectName })
  }
  const publish = async (id) => {
    await api.post(`/admin/projects/${id}/publish`)
    message.success('已发布公示')
    load()
  }
  const del = async (id) => {
    await api.delete(`/admin/projects/${id}`)
    message.success('已删除')
    load()
  }

  const openCreate = () => {
    form.resetFields()
    form.setFieldsValue({
      typeCode: 'ACADEMIC',
      projectName: '',
      academicYearId: years.find((y) => y.status === 'ACTIVE')?.id,
      levels: [
        { levelName: '一等', levelOrder: 1, allocType: 'ratio', ratio: 10, amount: 12000 },
        { levelName: '二等', levelOrder: 2, allocType: 'ratio', ratio: 20, amount: 8000 },
        { levelName: '三等', levelOrder: 3, allocType: 'ratio', ratio: 40, amount: 4000 }
      ]
    })
    setEditingId(null)
    setVisible(true)
  }

  return (
    <Card title="奖学金项目" extra={<Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>新建项目</Button>}>
      {list.length === 0 && <div style={{ color: '#8c8c8c', padding: 24 }}>暂无项目，点击「新建项目」开始创建。</div>}
      <Space direction="vertical" size="large" style={{ width: '100%' }}>
        {list.map((row) => {
          const p = row.project
          const year = years.find((y) => y.id === p.academicYearId)
          return (
            <Card key={p.id} type="inner" title={<Space><span>{p.projectName}</span><Tag color="blue">{p.typeCode}</Tag><Tag>{year?.yearName}</Tag></Space>}
                  extra={
                    <Space>
                      <Tag color={
                        p.status === 'OPEN' ? 'blue' :
                          p.status === 'REVIEWING' ? 'gold' :
                            p.status === 'PUBLISHED' ? 'green' : 'default'
                      }>{p.status}</Tag>
                      <Button onClick={() => openEdit(row)}>编辑项目</Button>
                      <Button onClick={() => triggerRank(p.id, p.projectName)} type="primary">执行排名与等级分配</Button>
                      {p.ranked && (
                        <Button icon={<TrophyOutlined />} onClick={() => showAwardPreview(p.id, p.projectName)}>获奖名单</Button>
                      )}
                      <Popconfirm title="发布公示？" onConfirm={() => publish(p.id)}><Button>发布公示</Button></Popconfirm>
                      <Popconfirm title="确定删除该项目？将一并删除等级配置和申请。" onConfirm={() => del(p.id)}>
                        <Button danger icon={<DeleteOutlined />} />
                      </Popconfirm>
                    </Space>
                  }>
              <Descriptions size="small" column={2}>
                <Descriptions.Item label="项目说明" span={2}>{p.description || '—'}</Descriptions.Item>
                <Descriptions.Item label="已执行排名">{p.ranked ? <Tag color="green">是</Tag> : <Tag>否</Tag>}</Descriptions.Item>
              </Descriptions>
              <Divider style={{ margin: '12px 0' }} />
              <div style={{ fontWeight: 600, marginBottom: 8 }}>等级与比例</div>
              <Table size="small" pagination={false} rowKey="id" dataSource={row.levels} columns={[
                { title: '等级', dataIndex: 'levelName' },
                { title: '比例', dataIndex: 'ratio', render: (v) => v ? `${v}%` : '—' },
                {
                  title: '名额计算方式',
                  render: (_, l) => l.quota != null
                    ? <Tag color="orange">固定 {l.quota} 人</Tag>
                    : l.ratio != null
                      ? <Tag color="blue">按比例（{l.ratio}%）</Tag>
                      : '—'
                },
                { title: '已分配', dataIndex: 'quota', render: (v) => v != null ? `${v} 人` : '—' },
                { title: '金额', dataIndex: 'amount', render: (v) => v ? `${v} 元` : '—' }
              ]} />
              {row.criteria.length > 0 && <>
                <Divider style={{ margin: '12px 0' }} />
                <div style={{ fontWeight: 600, marginBottom: 8 }}>申请条件</div>
                <Space wrap>
                  {row.criteria.map((c) => <Tag key={c.id} color="purple">{c.ruleType}: {c.ruleValue}</Tag>)}
                </Space>
              </>}
            </Card>
          )
        })}
      </Space>

      {/* 获奖名单预览 Modal */}
      <AwardPreviewModal
        open={awardModal.open}
        projectName={awardModal.projectName}
        data={awardModal.data}
        onClose={() => setAwardModal({ open: false, data: null, projectName: '' })}
      />

      <Modal open={visible} title={editingId ? '编辑奖学金项目' : '新建奖学金项目'}
             onCancel={() => { setVisible(false); setEditingId(null) }}
             onOk={save} width={760} destroyOnClose
             okText={editingId ? '保存修改' : '创建'}>
        <Form form={form} layout="vertical">
          <Form.Item name="academicYearId" label="学年" rules={[{ required: true }]}>
            <Select options={years.map((y) => ({ value: y.id, label: y.yearName }))} />
          </Form.Item>
          <Form.Item name="typeCode" label="奖学金类型" rules={[{ required: true }]}>
            <Select options={SCHOLARSHIP_TYPES} />
          </Form.Item>
          <Form.Item name="projectName" label="项目名称" rules={[{ required: true }]}>
            <Input placeholder="例：2025-2026 学年学业奖学金" />
          </Form.Item>
          <Form.Item name="description" label="项目说明">
            <Input.TextArea rows={2} />
          </Form.Item>

          <Divider>等级与比例（按综测排名比例分配）</Divider>
          <Form.List name="levels">
            {(fields, { add, remove }) => (
              <>
                {fields.map(({ key, name }) => (
                  <LevelRow key={key} name={name} form={form} onRemove={() => remove(name)} />
                ))}
                <Button type="dashed" onClick={() => add({ levelName: '', levelOrder: fields.length + 1, allocType: 'ratio', ratio: 10, amount: null })} block>+ 增加等级</Button>
              </>
            )}
          </Form.List>

          <Divider>申请条件（可选）</Divider>
          <Form.List name="criteria">
            {(fields, { add, remove }) => (
              <>
                {fields.map(({ key, name }) => (
                  <Space key={key} align="baseline" style={{ display: 'flex', marginBottom: 8 }}>
                    <Form.Item name={[name, 'ruleType']} rules={[{ required: true }]} style={{ width: 240 }}>
                      <Select options={[
                        { value: 'TOTAL_RANK_TOP_RATIO', label: '综测总分排名前 N%' },
                        { value: 'ACADEMIC_MIN', label: '智育下限 ≥' },
                        { value: 'NO_DISCIPLINE', label: '无处分（值=1）' },
                        { value: 'DEGREE_TYPE', label: '培养类型（ACADEMIC/PROFESSIONAL）' },
                        { value: 'GRADE', label: '年级（研二/研三）' },
                        { value: 'INNOVATION_MIN', label: '创新分下限 ≥' }
                      ]} placeholder="规则类型" />
                    </Form.Item>
                    <Form.Item name={[name, 'ruleValue']} rules={[{ required: true }]} style={{ width: 200 }}>
                      <Input placeholder="规则值（如 70 / 80 / ACADEMIC）" />
                    </Form.Item>
                    <a onClick={() => remove(name)}>删除</a>
                  </Space>
                ))}
                <Button type="dashed" onClick={() => add()} block>+ 添加条件</Button>
              </>
            )}
          </Form.List>
        </Form>
      </Modal>
    </Card>
  )
}

// =================== 等级行组件（分配方式选择） ===================

function LevelRow({ name, form, onRemove }) {
  const allocType = Form.useWatch(['levels', name, 'allocType'], form)

  return (
    <div style={{
      display: 'flex', gap: 8, alignItems: 'flex-start',
      marginBottom: 8, padding: '10px 12px',
      background: '#fafafa', borderRadius: 6, border: '1px solid #f0f0f0'
    }}>
      {/* 等级名称 */}
      <Form.Item name={[name, 'levelName']} label="等级名称" rules={[{ required: true }]}
                 style={{ marginBottom: 0, width: 130 }}>
        <Input placeholder="如：一等奖" />
      </Form.Item>

      {/* 排序 */}
      <Form.Item name={[name, 'levelOrder']} label="排序" rules={[{ required: true }]}
                 style={{ marginBottom: 0, width: 80 }}>
        <InputNumber min={1} style={{ width: '100%' }} />
      </Form.Item>

      {/* 分配方式 */}
      <Form.Item name={[name, 'allocType']} label="分配方式" rules={[{ required: true, message: '请选择分配方式' }]}
                 style={{ marginBottom: 0, width: 170 }}>
        <Select
          placeholder="请选择"
          options={[
            { value: 'ratio', label: '按综测排名比例' },
            { value: 'fixed', label: '按固定名额' }
          ]}
          onChange={() => {
            // 切换时清除另一方的值
            const levels = form.getFieldValue('levels')
            levels[name].ratio = null
            levels[name].quota = null
            form.setFieldValue('levels', levels)
          }}
        />
      </Form.Item>

      {/* 比例输入（仅比例模式显示） */}
      {allocType === 'ratio' && (
        <Form.Item name={[name, 'ratio']} label="比例" rules={[{ required: true, message: '请填写比例' }]}
                   extra="如填 10 表示前 10% 获奖"
                   style={{ marginBottom: 0, width: 150 }}>
          <InputNumber min={0} max={100} addonAfter="%" style={{ width: '100%' }} placeholder="如 10" />
        </Form.Item>
      )}

      {/* 固定名额输入（仅固定模式显示） */}
      {allocType === 'fixed' && (
        <Form.Item name={[name, 'quota']} label="固定名额" rules={[{ required: true, message: '请填写名额' }]}
                   extra="固定授予人数，与总人数无关"
                   style={{ marginBottom: 0, width: 150 }}>
          <InputNumber min={1} addonAfter="人" style={{ width: '100%' }} placeholder="如 5" />
        </Form.Item>
      )}

      {/* 奖励金额 */}
      <Form.Item name={[name, 'amount']} label="奖励金额"
                 style={{ marginBottom: 0, width: 160 }}>
        <InputNumber min={0} addonAfter="元" style={{ width: '100%' }} placeholder="选填" />
      </Form.Item>

      <div style={{ paddingTop: 28 }}>
        <a style={{ color: '#ff4d4f' }} onClick={onRemove}>删除</a>
      </div>
    </div>
  )
}

// =================== 获奖名单预览 ===================

const memberCols = [
  { title: '排名', dataIndex: ['application', 'snapshotRank'], width: 70,
    sorter: (a, b) => (a.application?.snapshotRank || 9999) - (b.application?.snapshotRank || 9999),
    render: (v) => v ? <Tag color="gold">第 {v} 名</Tag> : '—' },
  { title: '学号', dataIndex: ['student', 'studentNo'], width: 110 },
  { title: '姓名', dataIndex: ['student', 'name'], width: 80 },
  { title: '专业', dataIndex: ['student', 'major'] },
  { title: '总分 S', dataIndex: ['evaluation', 'totalScore'],
    sorter: (a, b) => Number(a.evaluation?.totalScore || 0) - Number(b.evaluation?.totalScore || 0),
    render: (v) => v ? <b>{Number(v).toFixed(2)}</b> : '—' }
]

const unassignedCols = [
  ...memberCols,
  { title: '未获奖原因', dataIndex: 'reason', width: 280,
    render: (v) => <span style={{ color: '#cf1322', fontSize: 12 }}>{v || '—'}</span> }
]

function AwardPreviewModal({ open, projectName, data, onClose }) {
  if (!data) return null

  const { levelGroups = [], unassigned = [], totalApplicants } = data

  const tabItems = levelGroups.map((g, idx) => ({
    key: String(g.level.id),
    label: (
      <span>
        {LEVEL_ICONS[idx] || '🏅'} {g.level.levelName}
        <Badge count={g.members.length} color={LEVEL_COLORS[idx] || 'blue'}
               style={{ marginLeft: 6 }} />
      </span>
    ),
    children: (
        <Table
          size="small"
          rowKey={(r) => r.application.id}
          dataSource={g.members}
          columns={memberCols}
          pagination={{ pageSize: 20, showSizeChanger: true, showTotal: (t) => `共 ${t} 人` }}
          locale={{ emptyText: '该等级暂无获奖学生' }}
          scroll={{ x: 600 }}
        />
    )
  }))

  if (unassigned.length > 0) {
    tabItems.push({
      key: 'unassigned',
      label: <span>未获奖 <Badge count={unassigned.length} color="default" style={{ marginLeft: 6 }} /></span>,
      children: (
        <Table
          size="small"
          rowKey={(r) => r.application.id}
          dataSource={unassigned}
          columns={unassignedCols}
          pagination={{ pageSize: 20, showSizeChanger: true, showTotal: (t) => `共 ${t} 人` }}
          scroll={{ x: 800 }}
        />
      )
    })
  }

  return (
    <Modal
      open={open}
      title={<span><TrophyOutlined style={{ color: '#faad14', marginRight: 8 }} />获奖名单 — {projectName}</span>}
      onCancel={onClose}
      footer={<Button onClick={onClose}>关闭</Button>}
      width={820}
      centered
    >
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={6}>
          <Statistic title="申报总人数" value={totalApplicants} />
        </Col>
        {levelGroups.map((g, idx) => (
          <Col span={6} key={g.level.id}>
            <Statistic
              title={`${LEVEL_ICONS[idx] || '🏅'} ${g.level.levelName}`}
              value={g.members.length}
              suffix="人"
              valueStyle={{ color: idx === 0 ? '#faad14' : idx === 1 ? '#8c8c8c' : '#cd7f32' }}
            />
          </Col>
        ))}
      </Row>
      <Divider style={{ margin: '0 0 16px' }} />
      {tabItems.length > 0
        ? <Tabs items={tabItems} />
        : <div style={{ color: '#8c8c8c', textAlign: 'center', padding: 32 }}>暂无申报数据，请先让学生提交申请后再执行排名。</div>
      }
    </Modal>
  )
}
