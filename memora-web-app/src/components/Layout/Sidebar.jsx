import { useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { IconFolder } from '@arco-design/web-react/icon'
import { useAuth } from '../../contexts/AuthContext'
import { useKnowledgeBaseNavigation } from '../../hooks/useKnowledgeBaseNavigation'
import styles from './Sidebar.module.css'

const Sidebar = ({ collapsed }) => {
  const location = useLocation()
  const { currentUser } = useAuth()
  const { knowledgeBases } = useKnowledgeBaseNavigation(currentUser.tenantId, {
    errorMessage: '加载知识库失败',
  })
  const [listExpanded, setListExpanded] = useState(false)
  const previewCount = 6
  const activeKnowledgeBase = knowledgeBases.find((kb) => location.pathname === `/kb/${kb.id}`)
  let visibleKnowledgeBases = knowledgeBases

  if (!collapsed && !listExpanded && knowledgeBases.length > previewCount) {
    visibleKnowledgeBases = knowledgeBases.slice(0, previewCount)
    if (activeKnowledgeBase && !visibleKnowledgeBases.some((item) => item.id === activeKnowledgeBase.id)) {
      visibleKnowledgeBases = [...visibleKnowledgeBases.slice(0, previewCount - 1), activeKnowledgeBase]
    }
  }

  const hiddenCount = Math.max(knowledgeBases.length - visibleKnowledgeBases.length, 0)

  return (
    <aside className={`${styles.sidebar} ${collapsed ? styles.collapsed : ''}`}>
      {!collapsed && (
        <div className={styles.workspaceHeader}>
          <div className={styles.workspaceEyebrow}>当前工作区</div>
          <div className={styles.workspaceTitle}>{currentUser.tenantName}</div>
          <div className={styles.workspaceMeta}>{knowledgeBases.length} 个知识库</div>
        </div>
      )}

      <div className={styles.sectionTitle}>知识库列表</div>
      <div className={styles.list}>
        {visibleKnowledgeBases.map((kb) => (
          <Link
            key={kb.id}
            to={`/kb/${kb.id}`}
            className={`${styles.item} ${location.pathname === `/kb/${kb.id}` ? styles.active : ''}`}
          >
            <div className={styles.itemIcon}>
              <IconFolder />
            </div>
            {!collapsed && (
              <div className={styles.itemContent}>
                <div className={styles.itemTitle}>{kb.name}</div>
                <div className={styles.itemMeta}>
                  <span>{kb.documentCount || 0} 篇文档</span>
                  <span className={styles.itemMetaDivider} />
                  <span>{kb.syncEnabled ? '已同步连接' : '手工维护'}</span>
                </div>
              </div>
            )}
          </Link>
        ))}
      </div>

      {!collapsed && knowledgeBases.length > previewCount && (
        <button
          type="button"
          className={styles.listToggle}
          onClick={() => setListExpanded((current) => !current)}
        >
          {listExpanded ? '收起列表' : `展开更多${hiddenCount > 0 ? `（+${hiddenCount}）` : ''}`}
        </button>
      )}
    </aside>
  )
}

export default Sidebar
