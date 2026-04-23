import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import dayjs from 'dayjs'
import { useAuth } from '../../contexts/AuthContext'
import KnowledgeBaseFormModal from '../../components/KnowledgeBase/KnowledgeBaseFormModal'
import { knowledgeBaseApi } from '../../services/api/knowledgeBaseApi'
import { workspaceApi } from '../../services/api/workspaceApi'
import { emitKnowledgeBasesChanged } from '../../utils/knowledgeBaseEvents'
import styles from './Home.module.css'

const STATUS_LABELS = {
  SYNCED: '已同步连接',
  PENDING: '待同步',
  DISABLED: '未启用',
  IDLE: '空闲',
}

const Home = () => {
  const navigate = useNavigate()
  const { currentUser } = useAuth()
  const [dashboard, setDashboard] = useState(null)
  const [loading, setLoading] = useState(true)
  const [modalOpen, setModalOpen] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [modalError, setModalError] = useState('')
  const [feedback, setFeedback] = useState(null)
  const canCreateKnowledgeBase = ['OWNER', 'ADMIN', 'EDITOR'].includes(currentUser.role)

  const loadDashboard = async () => {
    try {
      setLoading(true)
      const response = await workspaceApi.getCurrentDashboard()
      if (response.code === 200) {
        setDashboard(response.data)
      }
    } catch (error) {
      console.error('加载工作台失败', error)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadDashboard()
  }, [currentUser.id, currentUser.tenantId])

  const handleSubmitKnowledgeBase = async (formData) => {
    try {
      setSubmitting(true)
      setModalError('')
      const response = await knowledgeBaseApi.createKnowledgeBase({
        ...formData,
        tenantId: currentUser.tenantId,
      })
      const createdKnowledgeBaseId = response?.data?.id
      setFeedback({ type: 'success', message: `知识库“${formData.name}”已创建` })
      setModalOpen(false)
      await loadDashboard()
      emitKnowledgeBasesChanged()
      if (createdKnowledgeBaseId) {
        navigate(`/kb/${createdKnowledgeBaseId}`)
      }
    } catch (error) {
      console.error('保存知识库失败', error)
      setModalError(error?.message || '保存知识库失败，请稍后重试')
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) {
    return <div className={styles.state}>正在加载工作台...</div>
  }

  if (!dashboard) {
    return <div className={styles.state}>当前工作台不可用</div>
  }

  const knowledgeBases = dashboard.knowledgeBases || []
  const recentDocuments = dashboard.recentDocuments || []
  const knowledgeBaseMap = new Map(knowledgeBases.map((item) => [item.id, item]))

  return (
    <div className={styles.page}>
      <section className={styles.workspaceBar}>
        <div className={styles.workspaceMain}>
          <div className={styles.eyebrow}>当前工作区</div>
          <div className={styles.workspaceIdentity}>
            <h1 className={styles.title}>{dashboard.workspace.name}</h1>
            <span className={styles.workspaceMeta}>{knowledgeBases.length} 个知识库</span>
          </div>
        </div>
        {canCreateKnowledgeBase ? (
          <button type="button" className={styles.primaryButton} onClick={() => setModalOpen(true)}>
            新建知识库
          </button>
        ) : null}
      </section>

      {feedback && (
        <div className={`${styles.feedback} ${feedback.type === 'error' ? styles.feedbackError : styles.feedbackSuccess}`}>
          {feedback.message}
        </div>
      )}

      <section className={styles.contentGrid}>
        <section className={`${styles.panel} ${styles.recentPanel}`}>
          <div className={styles.panelHeader}>
            <div>
              <h2>最近编辑</h2>
            </div>
            <span>{recentDocuments.length}</span>
          </div>
          <div className={styles.documentList}>
            {recentDocuments.length > 0 ? (
              recentDocuments.map((document) => {
                const knowledgeBase = knowledgeBaseMap.get(document.knowledgeBaseId)

                return (
                  <article
                    key={document.id}
                    className={styles.documentItem}
                    onClick={() => navigate(`/docs/${document.id}/edit`)}
                  >
                    <div className={styles.documentMark} aria-hidden="true" />
                    <div className={`${styles.itemBody} ${styles.documentBody}`}>
                      <div className={styles.itemTitle}>{document.title}</div>
                      <div className={styles.itemMeta}>
                        <span>{knowledgeBase?.name || `知识库 #${document.knowledgeBaseId}`}</span>
                        <span className={styles.metaDivider} aria-hidden="true" />
                        <span>{dayjs(document.updatedAt).format('MM-DD HH:mm')}</span>
                      </div>
                    </div>
                    <div className={styles.itemTail} aria-hidden="true">›</div>
                  </article>
                )
              })
            ) : (
              <div className={styles.emptyState}>
                <strong>还没有最近编辑</strong>
                <p>进入知识库后开始写第一篇文档。</p>
              </div>
            )}
          </div>
        </section>

        <section className={`${styles.panel} ${styles.knowledgePanel}`}>
          <div className={styles.panelHeader}>
            <div>
              <h2>知识库列表</h2>
            </div>
            <span>{knowledgeBases.length}</span>
          </div>
          <div className={styles.knowledgeList}>
            {knowledgeBases.length > 0 ? (
              knowledgeBases.map((knowledgeBase) => (
                <article
                  key={knowledgeBase.id}
                  className={styles.knowledgeItem}
                  onClick={() => navigate(`/kb/${knowledgeBase.id}`)}
                >
                  <div className={styles.knowledgeMark} aria-hidden="true" />
                  <div className={`${styles.itemBody} ${styles.knowledgeBody}`}>
                    <div className={styles.itemHead}>
                      <div className={styles.itemTitle}>{knowledgeBase.name}</div>
                    </div>
                    <div className={styles.itemMeta}>
                      <span>{knowledgeBase.documentCount} 篇文档</span>
                      <span className={styles.metaDivider} aria-hidden="true" />
                      <span>
                        {knowledgeBase.syncEnabled
                          ? STATUS_LABELS[knowledgeBase.syncStatus] || knowledgeBase.syncStatus
                          : '手工维护'}
                      </span>
                    </div>
                  </div>
                  <div className={styles.itemTail} aria-hidden="true">›</div>
                </article>
              ))
            ) : (
              <div className={styles.emptyState}>
                <strong>还没有知识库</strong>
                <p>先创建一个知识库，再开始整理文档。</p>
              </div>
            )}
          </div>
        </section>
      </section>

      <KnowledgeBaseFormModal
        mode="create"
        open={modalOpen}
        initialValues={null}
        submitting={submitting}
        errorMessage={modalError}
        onClose={() => {
          setModalError('')
          setModalOpen(false)
        }}
        onSubmit={handleSubmitKnowledgeBase}
      />
    </div>
  )
}

export default Home
