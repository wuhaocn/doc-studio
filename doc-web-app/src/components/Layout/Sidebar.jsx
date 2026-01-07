import { useEffect, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { IconFolder, IconFile, IconPlus } from '@arco-design/web-react/icon'
import { knowledgeBaseApi } from '../../services/api/knowledgeBaseApi'
import styles from './Sidebar.module.css'

const Sidebar = ({ collapsed }) => {
  const location = useLocation()
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

  const isActive = (kbId) => {
    return location.pathname.startsWith(`/kb/${kbId}`)
  }

  return (
    <aside className={`${styles.sidebar} ${collapsed ? styles.collapsed : ''}`}>
      <div className={styles.header}>
        <h3 className={styles.title}>知识库</h3>
        <button className={styles.newBtn} title="新建知识库">
          <IconPlus />
        </button>
      </div>
      <div className={styles.content}>
        {loading ? (
          <div className={styles.loading}>加载中...</div>
        ) : (
          <ul className={styles.list}>
            {knowledgeBases.map((kb) => (
              <li key={kb.id} className={styles.item}>
                <Link
                  to={`/kb/${kb.id}`}
                  className={`${styles.link} ${isActive(kb.id) ? styles.active : ''}`}
                >
                  <IconFolder className={styles.icon} />
                  <span className={styles.name}>{kb.name}</span>
                  <span className={styles.count}>({kb.documentCount})</span>
                </Link>
              </li>
            ))}
          </ul>
        )}
      </div>
    </aside>
  )
}

export default Sidebar

