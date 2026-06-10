import { useEffect, useState } from 'react'
import { Card, Tabs, Table, Button, Modal, Input, message, Space, Empty, Image, Tag } from 'antd'
import { EyeOutlined } from '@ant-design/icons'
import api from '../../api.js'

const STATUS_TAG = (s) => {
  const map = { PENDING: ['gold', '待审核'], APPROVED: ['green', '已通过'], REJECTED: ['red', '已驳回'] }
  const [color, text] = map[s] || ['default', s]
  return <Tag color={color}>{text}</Tag>
}

const ITEM_KINDS = [
  { key: 'moralRecords', label: '品德记实', endpoint: 'moral-record' },
  { key: 'riItems', label: '研究创新', endpoint: 'ri' },
  { key: 'psItems', label: '专业技能', endpoint: 'ps' },
  { key: 'owItems', label: '组织工作', endpoint: 'ow' },
  { key: 'saItems', label: '体育美育', endpoint: 'sa' },
  { key: 'lpItems', label: '劳动实践', endpoint: 'lp' }
]

export default function Review() {
  const [pending, setPending] = useState({ moralRecords:[], riItems:[], psItems:[], owItems:[], saItems:[], lpItems:[] })
  const [preview, setPreview] = useState({ visible: false, url: '' })

  const loadPending = async () => setPending(await api.get('/counselor/items/pending'))

  useEffect(() => { loadPending() }, [])

  const review = async (kind, id, status) => {
    if (status === 'REJECTED') {
      let remark = ''
      Modal.confirm({
        title: '请填写驳回原因',
        content: <Input onChange={(e) => { remark = e.target.value }} placeholder="驳回原因" />,
        onOk: async () => {
          if (!remark.trim()) { message.warning('请填写驳回原因'); return Promise.reject() }
          await api.post(`/counselor/items/${kind}/${id}/review`, { status, remark })
          message.success('已驳回'); loadPending()
        }
      })
      return
    }
    await api.post(`/counselor/items/${kind}/${id}/review`, { status })
    message.success('已通过'); loadPending()
  }

  const buildCols = (kind, showActions) => [
    { title: '学生', width: 140, render: (_, r) => `${r.student?.name}（${r.student?.studentNo}）` },
    { title: '专业', width: 110, render: (_, r) => r.student?.major },
    { title: '类型', dataIndex: ['item','itemType'], width: 120 },
    { title: '名称/说明', render: (_, r) => r.item.name || r.item.description },
    { title: '分值', dataIndex: ['item','score'], width: 80, render: v => v ? Number(v).toFixed(2) : '—' },
    { title: '审核', dataIndex: ['item','reviewStatus'], width: 90, render: v => STATUS_TAG(v) },
    { title: '驳回原因', dataIndex: ['item','reviewRemark'], width: 140, render: (v, r) => r.item.reviewStatus === 'REJECTED' ? <span style={{color:'#cf1322'}}>{v||'—'}</span> : null },
    { title: '附件', width: 90, render: (_, r) => { const url = r.item.attachmentUrl; return url ? <Button size="small" icon={<EyeOutlined />} onClick={() => setPreview({visible:true,url})}>查看</Button> : <Tag color="orange">无</Tag> } },
    showActions && { title: '操作', width: 140, render: (_, r) => (
      <Space>
        <Button size="small" type="primary" onClick={() => review(ITEM_KINDS.find(k=>k.key===kind)?.endpoint||kind, r.item.id, 'APPROVED')}>通过</Button>
        <Button size="small" danger onClick={() => review(ITEM_KINDS.find(k=>k.key===kind)?.endpoint||kind, r.item.id, 'REJECTED')}>驳回</Button>
      </Space>
    )}
  ].filter(Boolean)

  const PAGE_CFG = { showSizeChanger: true, pageSizeOptions: ['10','20','50'], defaultPageSize: 20, showTotal: total => `共 ${total} 条` }

  const renderTable = (kind, list, showActions) => list.length === 0 ? <Empty description="暂无" /> : (
    <Table size="small" rowKey={r => r.item.id} dataSource={list} columns={buildCols(kind, showActions)} pagination={PAGE_CFG} scroll={{x:900}}
           rowClassName={r => r.item.reviewStatus === 'REJECTED' ? 'row-rejected' : ''} />
  )

  const pendingTotal = ITEM_KINDS.reduce((s, k) => s + (pending[k.key]?.length||0), 0)

  return (
    <>
      <Card title="综测材料审核">
        <Tabs defaultActiveKey="pending" items={[{
          key: 'pending',
          label: `待审核（${pendingTotal}）`,
          children: (
            <Tabs items={ITEM_KINDS.map(k => ({
              key: k.key,
              label: `${k.label}（${pending[k.key]?.length||0}）`,
              children: renderTable(k.key, pending[k.key]||[], true)
            }))} />
          )
        }]} />
      </Card>
      <Modal open={preview.visible} title="证明材料" footer={null}
             onCancel={() => setPreview({visible:false,url:''})} width={700} centered>
        {preview.url && (preview.url.match(/\.(jpg|jpeg|png|gif|webp|bmp)$/i)
          ? <Image src={preview.url} style={{width:'100%',maxHeight:'70vh',objectFit:'contain'}} preview={false} />
          : <div style={{textAlign:'center',padding:24}}><a href={preview.url} target="_blank">打开文件</a></div>)}
      </Modal>
    </>
  )
}
