import { useEffect } from 'react'
import dayjs from 'dayjs'
import { useOutletContext } from 'react-router-dom'
import PageState from '../../components/Feedback/PageState'
import { useDocumentTitle } from '../../hooks/useDocumentTitle'
import styles from './WorkspaceManage.module.css'

const WorkspaceManageTrashPage = () => {
  const {
    canManageKnowledgeBaseTrash,
    knowledgeBases,
    deletedKnowledgeBases,
    knowledgeBaseTrashLoading,
    knowledgeBaseTrashError,
    knowledgeBaseTrashLoaded,
    restoringKnowledgeBaseId,
    loadDeletedKnowledgeBases,
    handleRestoreKnowledgeBase,
  } = useOutletContext()
  useDocumentTitle('知识库回收站')

  useEffect(() => {
    if (canManageKnowledgeBaseTrash && !knowledgeBaseTrashLoaded) {
      void loadDeletedKnowledgeBases()
    }
  }, [canManageKnowledgeBaseTrash, knowledgeBaseTrashLoaded, loadDeletedKnowledgeBases])

  if (!canManageKnowledgeBaseTrash) {
    return (
      <PageState
        title="当前角色不能管理知识库回收站"
        description="仅 OWNER / ADMIN 可管理。"
      />
    )
  }

  const deletedKnowledgeBaseCount = deletedKnowledgeBases.length
  const deletedDocumentCount = deletedKnowledgeBases.reduce((sum, item) => sum + (item.documentCount || 0), 0)

  return (
    <div className={styles.stack}>
      <section className={styles.surfaceCard}>
        <div className={styles.sectionHeader}>
          <div>
            <h2 className={styles.sectionTitle}>知识库回收站</h2>
            <span className={styles.sectionMeta}>恢复已删除的知识库。</span>
          </div>
          <div className={styles.sectionActions}>
            <button
              type="button"
              className={styles.secondaryButton}
              onClick={loadDeletedKnowledgeBases}
              disabled={knowledgeBaseTrashLoading}
            >
              {knowledgeBaseTrashLoading ? '刷新中...' : '刷新'}
            </button>
          </div>
        </div>

        <div className={styles.metricsGrid}>
          <article className={styles.metricCard}>
            <span>已删除知识库</span>
            <strong>{deletedKnowledgeBaseCount}</strong>
          </article>
          <article className={styles.metricCard}>
            <span>已删除文档</span>
            <strong>{deletedDocumentCount}</strong>
          </article>
          <article className={styles.metricCard}>
            <span>当前知识库总数</span>
            <strong>{knowledgeBases.length}</strong>
          </article>
        </div>

        {knowledgeBaseTrashError ? <div className={styles.errorState}>{knowledgeBaseTrashError}</div> : null}

        {knowledgeBaseTrashLoading && deletedKnowledgeBases.length === 0 ? (
          <div className={styles.emptyState}>
            <strong>正在加载回收站</strong>
          </div>
        ) : deletedKnowledgeBases.length > 0 ? (
          <div className={styles.list}>
            {deletedKnowledgeBases.map((knowledgeBase) => (
              <article key={knowledgeBase.id} className={styles.itemCard}>
                <div className={styles.itemMain}>
                  <div className={styles.itemTopline}>
                    <strong className={styles.itemTitle}>{knowledgeBase.name}</strong>
                  </div>
                  <div className={styles.itemMeta}>
                    <span>{knowledgeBase.description || `标识：${knowledgeBase.slug || '未设置'}`}</span>
                    <span>{knowledgeBase.documentCount || 0} 篇文档</span>
                    {knowledgeBase.deletedAt ? <span>删除于 {dayjs(knowledgeBase.deletedAt).format('YYYY-MM-DD HH:mm')}</span> : null}
                  </div>
                </div>
                <div className={styles.sectionActions}>
                  <button
                    type="button"
                    className={styles.primaryButton}
                    onClick={() => handleRestoreKnowledgeBase(knowledgeBase.id)}
                    disabled={restoringKnowledgeBaseId === knowledgeBase.id}
                  >
                    {restoringKnowledgeBaseId === knowledgeBase.id ? '恢复中...' : '恢复'}
                  </button>
                </div>
              </article>
            ))}
          </div>
        ) : (
          <div className={styles.emptyState}>
            <strong>回收站为空</strong>
          </div>
        )}
      </section>
    </div>
  )
}

export default WorkspaceManageTrashPage
