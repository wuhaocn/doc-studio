import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { Card, Button, Empty, Spin, Input, Dropdown, Menu } from '@arco-design/web-react'
import { 
  IconPlus, 
  IconFolder, 
  IconSearch, 
  IconMore,
  IconEdit,
  IconDelete,
  IconShareAlt,
  IconSettings
} from '@arco-design/web-react/icon'
import { knowledgeBaseApi } from '../../services/api/knowledgeBaseApi'
import styles from './Home.module.css'
import dayjs from 'dayjs'

const Home = () => {
  const navigate = useNavigate()
  const [knowledgeBases, setKnowledgeBases] = useState([])
  const [loading, setLoading] = useState(true)
  const [searchKeyword, setSearchKeyword] = useState('')
  const [viewMode, setViewMode] = useState('grid') // grid | list

  useEffect(() => {
    loadKnowledgeBases()
  }, [])

  const loadKnowledgeBases = async () => {
    try {
      setLoading(true)
      // 使用不分页的接口获取用户的知识库列表
      const response = await knowledgeBaseApi.getKnowledgeBasesByUserId(1)
      if (response.code === 200) {
        setKnowledgeBases(response.data || [])
      }
    } catch (error) {
      console.error('加载知识库列表失败:', error)
      // 如果出错，尝试使用分页接口
      try {
        const response = await knowledgeBaseApi.getKnowledgeBases({ page: 1, size: 100 })
        if (response.code === 200) {
          // 处理分页数据
          const data = response.data?.records || response.data || []
          setKnowledgeBases(data)
        }
      } catch (err) {
        console.error('加载知识库列表失败:', err)
      }
    } finally {
      setLoading(false)
    }
  }

  const handleCreateKnowledgeBase = () => {
    // TODO: 打开创建知识库弹窗
    console.log('创建知识库')
  }

  const handleKnowledgeBaseClick = (kbId) => {
    navigate(`/kb/${kbId}`)
  }

  const handleKnowledgeBaseMenuClick = (e, kbId) => {
    e.stopPropagation()
    // TODO: 实现菜单操作
    console.log('菜单操作:', e.key, kbId)
  }

  const filteredKnowledgeBases = knowledgeBases.filter(kb => 
    kb.name.toLowerCase().includes(searchKeyword.toLowerCase()) ||
    (kb.description && kb.description.toLowerCase().includes(searchKeyword.toLowerCase()))
  )

  return (
    <div className={styles.home}>
      <div className={styles.header}>
        <div className={styles.headerLeft}>
          <h1 className={styles.title}>知识库</h1>
          <div className={styles.searchWrapper}>
            <Input
              prefix={<IconSearch />}
              placeholder="搜索知识库..."
              value={searchKeyword}
              onChange={setSearchKeyword}
              className={styles.searchInput}
              allowClear
            />
          </div>
        </div>
        <div className={styles.headerRight}>
          <Button 
            type="primary" 
            icon={<IconPlus />} 
            onClick={handleCreateKnowledgeBase}
            className={styles.createButton}
          >
            新建知识库
          </Button>
        </div>
      </div>

      {loading ? (
        <div className={styles.loading}>
          <Spin size="large" dot />
        </div>
      ) : knowledgeBases.length === 0 ? (
        <div className={styles.empty}>
          <Empty
            description={
              <span style={{ color: 'var(--color-text-secondary)', fontSize: '15px' }}>
                还没有知识库，创建一个开始吧
              </span>
            }
            icon={<IconFolder style={{ fontSize: 64, color: 'var(--color-text-tertiary)' }} />}
          >
            <Button 
              type="primary" 
              size="large"
              icon={<IconPlus />}
              onClick={handleCreateKnowledgeBase}
              style={{ marginTop: '16px' }}
            >
              创建知识库
            </Button>
          </Empty>
        </div>
      ) : (
        <>
          {filteredKnowledgeBases.length === 0 ? (
            <div className={styles.empty}>
              <Empty
                description={
                  <span style={{ color: 'var(--color-text-secondary)', fontSize: '15px' }}>
                    {searchKeyword ? '没有找到匹配的知识库' : '还没有知识库，创建一个开始吧'}
                  </span>
                }
                icon={<IconFolder style={{ fontSize: 64, color: 'var(--color-text-tertiary)' }} />}
              >
                {!searchKeyword && (
                  <Button 
                    type="primary" 
                    size="large"
                    icon={<IconPlus />}
                    onClick={handleCreateKnowledgeBase}
                    style={{ marginTop: '16px' }}
                  >
                    创建知识库
                  </Button>
                )}
              </Empty>
            </div>
          ) : (
            <div className={viewMode === 'grid' ? styles.grid : styles.list}>
              {filteredKnowledgeBases.map((kb) => {
                const menuItems = (
                  <Menu onClick={(e) => handleKnowledgeBaseMenuClick(e, kb.id)}>
                    <Menu.Item key="edit">
                      <IconEdit /> 编辑
                    </Menu.Item>
                    <Menu.Item key="share">
                      <IconShareAlt /> 分享
                    </Menu.Item>
                    <Menu.Item key="settings">
                      <IconSettings /> 设置
                    </Menu.Item>
                    <Menu.Item key="divider" disabled style={{ height: '1px', padding: 0, margin: '4px 0', background: 'var(--color-border-light)' }}>
                      <div style={{ height: '1px', background: 'var(--color-border-light)' }} />
                    </Menu.Item>
                    <Menu.Item key="delete" style={{ color: 'var(--color-danger)' }}>
                      <IconDelete /> 删除
                    </Menu.Item>
                  </Menu>
                )

                return (
                  <Card
                    key={kb.id}
                    className={styles.card}
                    hoverable
                    onClick={() => handleKnowledgeBaseClick(kb.id)}
                  >
                    <div className={styles.cardHeader}>
                      <div className={styles.cardIconWrapper}>
                        <IconFolder className={styles.folderIcon} />
                      </div>
                      <div className={styles.cardContent}>
                        <div className={styles.cardTitleRow}>
                          <h3 className={styles.cardTitle}>{kb.name}</h3>
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
                        {kb.description && (
                          <p className={styles.cardDescription}>{kb.description}</p>
                        )}
                      </div>
                    </div>
                    <div className={styles.cardFooter}>
                      <div className={styles.metaGroup}>
                        <span className={styles.meta}>
                          <span className={styles.metaValue}>{kb.documentCount}</span>
                          <span className={styles.metaLabel}>文档</span>
                        </span>
                        <span className={styles.metaDivider}>•</span>
                        <span className={styles.meta}>
                          <span className={styles.metaLabel}>更新于</span>
                          <span className={styles.metaValue}>{dayjs(kb.updatedAt).format('MM-DD')}</span>
                        </span>
                      </div>
                      {kb.isPublic && (
                        <span className={styles.publicTag}>公开</span>
                      )}
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

export default Home

