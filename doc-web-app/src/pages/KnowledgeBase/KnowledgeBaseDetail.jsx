import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { Card, Button, Empty, Spin, Breadcrumb, Tag } from '@arco-design/web-react'
import { IconHome, IconFile, IconPlus } from '@arco-design/web-react/icon'
import { knowledgeBaseApi } from '../../services/api/knowledgeBaseApi'
import { documentApi } from '../../services/api/documentApi'
import styles from './KnowledgeBaseDetail.module.css'
import dayjs from 'dayjs'

const KnowledgeBaseDetail = () => {
  const { id } = useParams()
  const navigate = useNavigate()
  const [knowledgeBase, setKnowledgeBase] = useState(null)
  const [documents, setDocuments] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    loadData()
  }, [id])

  const loadData = async () => {
    try {
      setLoading(true)
      const [kbResponse, docResponse] = await Promise.all([
        knowledgeBaseApi.getKnowledgeBaseById(id),
        documentApi.getDocumentsByKnowledgeBaseId(id),
      ])

      if (kbResponse.code === 200) {
        setKnowledgeBase(kbResponse.data)
      }
      if (docResponse.code === 200) {
        setDocuments(docResponse.data || [])
      }
    } catch (error) {
      console.error('加载数据失败:', error)
    } finally {
      setLoading(false)
    }
  }

  const handleCreateDocument = () => {
    navigate(`/kb/${id}/doc/new`)
  }

  const handleDocumentClick = (docId) => {
    navigate(`/kb/${id}/doc/${docId}`)
  }

  if (loading) {
    return (
      <div className={styles.loading}>
        <Spin size="large" />
      </div>
    )
  }

  if (!knowledgeBase) {
    return (
      <div className={styles.empty}>
        <Empty description="知识库不存在" />
      </div>
    )
  }

  return (
    <div className={styles.detail}>
      <Breadcrumb className={styles.breadcrumb}>
        <Breadcrumb.Item>
          <a href="/" onClick={(e) => { e.preventDefault(); navigate('/') }}>
            <IconHome /> 首页
          </a>
        </Breadcrumb.Item>
        <Breadcrumb.Item>{knowledgeBase.name}</Breadcrumb.Item>
      </Breadcrumb>

      <div className={styles.header}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '8px' }}>
            <h1 className={styles.title}>{knowledgeBase.name}</h1>
            {knowledgeBase.isPublic && (
              <Tag color="blue" size="small">公开</Tag>
            )}
          </div>
          {knowledgeBase.description && (
            <p className={styles.description}>{knowledgeBase.description}</p>
          )}
          <div style={{ display: 'flex', gap: '16px', marginTop: '12px', fontSize: '13px', color: 'var(--color-text-secondary)' }}>
            <span>{knowledgeBase.documentCount} 个文档</span>
            <span>•</span>
            <span>查看 {knowledgeBase.viewCount} 次</span>
          </div>
        </div>
        <Button type="primary" icon={<IconPlus />} onClick={handleCreateDocument}>
          新建文档
        </Button>
      </div>

      {documents.length === 0 ? (
        <div className={styles.empty}>
          <Empty
            description={
              <span style={{ color: 'var(--color-text-secondary)', fontSize: '15px' }}>
                还没有文档，创建一个开始吧
              </span>
            }
            icon={<IconFile style={{ fontSize: 64, color: 'var(--color-text-tertiary)' }} />}
          >
            <Button 
              type="primary" 
              size="large"
              icon={<IconPlus />}
              onClick={handleCreateDocument}
              style={{ marginTop: '16px' }}
            >
              创建文档
            </Button>
          </Empty>
        </div>
      ) : (
        <div className={styles.list}>
          {documents.map((doc) => (
            <Card
              key={doc.id}
              className={styles.card}
              hoverable
              onClick={() => handleDocumentClick(doc.id)}
            >
              <div className={styles.cardHeader}>
                <IconFile className={styles.fileIcon} />
                <h3 className={styles.cardTitle}>{doc.title}</h3>
              </div>
              {doc.contentText && (
                <p className={styles.cardPreview}>{doc.contentText.substring(0, 100)}...</p>
              )}
              <div className={styles.cardFooter}>
                <span className={styles.meta}>
                  更新于 {dayjs(doc.updatedAt).format('YYYY-MM-DD HH:mm')}
                </span>
                <span className={styles.meta}>查看 {doc.viewCount} 次</span>
              </div>
            </Card>
          ))}
        </div>
      )}
    </div>
  )
}

export default KnowledgeBaseDetail

