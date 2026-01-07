import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { Card, Button, Empty, Spin } from '@arco-design/web-react'
import { IconPlus, IconFolder } from '@arco-design/web-react/icon'
import { knowledgeBaseApi } from '../../services/api/knowledgeBaseApi'
import styles from './Home.module.css'
import dayjs from 'dayjs'

const Home = () => {
  const navigate = useNavigate()
  const [knowledgeBases, setKnowledgeBases] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    loadKnowledgeBases()
  }, [])

  const loadKnowledgeBases = async () => {
    try {
      setLoading(true)
      const response = await knowledgeBaseApi.getKnowledgeBases()
      if (response.code === 200) {
        setKnowledgeBases(response.data || [])
      }
    } catch (error) {
      console.error('加载知识库列表失败:', error)
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

  return (
    <div className={styles.home}>
      <div className={styles.header}>
        <h1 className={styles.title}>我的知识库</h1>
        <Button type="primary" icon={<IconPlus />} onClick={handleCreateKnowledgeBase}>
          新建知识库
        </Button>
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
        <div className={styles.grid}>
          {knowledgeBases.map((kb) => (
            <Card
              key={kb.id}
              className={styles.card}
              hoverable
              onClick={() => handleKnowledgeBaseClick(kb.id)}
            >
              <div className={styles.cardHeader}>
                <IconFolder className={styles.folderIcon} />
                <h3 className={styles.cardTitle}>{kb.name}</h3>
              </div>
              {kb.description && (
                <p className={styles.cardDescription}>{kb.description}</p>
              )}
              <div className={styles.cardFooter}>
                <span className={styles.meta}>
                  {kb.documentCount} 个文档
                </span>
                <span className={styles.meta}>
                  {dayjs(kb.updatedAt).format('YYYY-MM-DD')}
                </span>
              </div>
            </Card>
          ))}
        </div>
      )}
    </div>
  )
}

export default Home

