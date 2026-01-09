import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { Card, Button, Empty, Spin, Breadcrumb, Tag, Input, Dropdown, Menu } from '@arco-design/web-react'
import { 
  IconHome, 
  IconFile, 
  IconPlus, 
  IconSearch,
  IconMore,
  IconEdit,
  IconDelete,
  IconCopy,
  IconShareAlt
} from '@arco-design/web-react/icon'
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
  const [searchKeyword, setSearchKeyword] = useState('')

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
        // 处理分页数据或列表数据
        const docData = docResponse.data?.records || docResponse.data || []
        setDocuments(Array.isArray(docData) ? docData : [])
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

  const handleDocumentMenuClick = (e, docId) => {
    e.stopPropagation()
    // TODO: 实现文档菜单操作
    console.log('文档操作:', e.key, docId)
  }

  const filteredDocuments = documents.filter(doc =>
    doc.title.toLowerCase().includes(searchKeyword.toLowerCase()) ||
    (doc.contentText && doc.contentText.toLowerCase().includes(searchKeyword.toLowerCase()))
  )

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
        <div className={styles.headerInfo}>
          <div className={styles.titleRow}>
            <h1 className={styles.title}>{knowledgeBase.name}</h1>
            {knowledgeBase.isPublic && (
              <Tag color="blue" size="small" className={styles.publicTag}>公开</Tag>
            )}
          </div>
          {knowledgeBase.description && (
            <p className={styles.description}>{knowledgeBase.description}</p>
          )}
          <div className={styles.metaInfo}>
            <span className={styles.metaItem}>
              <span className={styles.metaValue}>{knowledgeBase.documentCount}</span>
              <span className={styles.metaLabel}>个文档</span>
            </span>
            <span className={styles.metaDivider}>•</span>
            <span className={styles.metaItem}>
              <span className={styles.metaLabel}>查看</span>
              <span className={styles.metaValue}>{knowledgeBase.viewCount}</span>
              <span className={styles.metaLabel}>次</span>
            </span>
          </div>
        </div>
        <div className={styles.headerActions}>
          <Button type="primary" icon={<IconPlus />} onClick={handleCreateDocument}>
            新建文档
          </Button>
        </div>
      </div>

      {documents.length > 0 && (
        <div className={styles.toolbar}>
          <div className={styles.searchWrapper}>
            <Input
              prefix={<IconSearch />}
              placeholder="搜索文档..."
              value={searchKeyword}
              onChange={setSearchKeyword}
              className={styles.searchInput}
              allowClear
            />
          </div>
        </div>
      )}

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
        <>
          {filteredDocuments.length === 0 ? (
            <div className={styles.empty}>
              <Empty
                description={
                  <span style={{ color: 'var(--color-text-secondary)', fontSize: '15px' }}>
                    {searchKeyword ? '没有找到匹配的文档' : '还没有文档，创建一个开始吧'}
                  </span>
                }
                icon={<IconFile style={{ fontSize: 64, color: 'var(--color-text-tertiary)' }} />}
              >
                {!searchKeyword && (
                  <Button 
                    type="primary" 
                    size="large"
                    icon={<IconPlus />}
                    onClick={handleCreateDocument}
                    style={{ marginTop: '16px' }}
                  >
                    创建文档
                  </Button>
                )}
              </Empty>
            </div>
          ) : (
            <div className={styles.list}>
              {filteredDocuments.map((doc) => {
                const menuItems = (
                  <Menu onClick={(e) => handleDocumentMenuClick(e, doc.id)}>
                    <Menu.Item key="edit">
                      <IconEdit /> 编辑
                    </Menu.Item>
                    <Menu.Item key="copy">
                      <IconCopy /> 复制
                    </Menu.Item>
                    <Menu.Item key="share">
                      <IconShareAlt /> 分享
                    </Menu.Item>
                    <Menu.Divider />
                    <Menu.Item key="delete" style={{ color: 'var(--color-danger)' }}>
                      <IconDelete /> 删除
                    </Menu.Item>
                  </Menu>
                )

                return (
                  <Card
                    key={doc.id}
                    className={styles.card}
                    hoverable
                    onClick={() => handleDocumentClick(doc.id)}
                  >
                    <div className={styles.cardContent}>
                      <div className={styles.cardHeader}>
                        <div className={styles.cardIconWrapper}>
                          <IconFile className={styles.fileIcon} />
                        </div>
                        <div className={styles.cardInfo}>
                          <div className={styles.cardTitleRow}>
                            <h3 className={styles.cardTitle}>{doc.title}</h3>
                            <Dropdown droplist={menuItems} trigger="click" position="br">
                              <Button
                                type="text"
                                size="mini"
                                icon={<IconMore />}
                                className={styles.cardMenu}
                                onClick={(e) => e.stopPropagation()}
                              />
                            </Dropdown>
                          </div>
                          {doc.contentText && (
                            <p className={styles.cardPreview}>{doc.contentText.substring(0, 120)}</p>
                          )}
                        </div>
                      </div>
                      <div className={styles.cardFooter}>
                        <div className={styles.metaGroup}>
                          <span className={styles.meta}>
                            <span className={styles.metaLabel}>更新于</span>
                            <span className={styles.metaValue}>{dayjs(doc.updatedAt).format('MM-DD HH:mm')}</span>
                          </span>
                          <span className={styles.metaDivider}>•</span>
                          <span className={styles.meta}>
                            <span className={styles.metaLabel}>查看</span>
                            <span className={styles.metaValue}>{doc.viewCount}</span>
                            <span className={styles.metaLabel}>次</span>
                          </span>
                        </div>
                      </div>
                    </div>
                  </Card>
                )
              })}
            </div>
          )}
        </>
      )}
    </div>
  )
}

export default KnowledgeBaseDetail

