import { useCallback, useEffect, useState } from 'react'
import dayjs from 'dayjs'
import { useNavigate } from 'react-router-dom'
import PageState from '../../components/Feedback/PageState'
import { useToast } from '../../components/Feedback/Toast'
import { DashboardSkeleton } from '../../components/Feedback/Skeleton'
import KnowledgeBaseFormModal from '../../components/KnowledgeBase/KnowledgeBaseFormModal'
import { useAuth } from '../../contexts/AuthContext'
import { useDocumentTitle } from '../../hooks/useDocumentTitle'
import { knowledgeBaseApi } from '../../services/api/knowledgeBaseApi'
import { workspaceApi } from '../../services/api/workspaceApi'
import { canCreateKnowledgeBaseForRole } from '../../utils/knowledgeBaseAccess'
import { emitKnowledgeBasesChanged, KNOWLEDGE_BASES_CHANGED_EVENT } from '../../utils/knowledgeBaseEvents'
import styles from './Home.module.css'

const Home = () => {
  const navigate = useNavigate()
  const { currentUser } = useAuth()
  const toast = useToast()
  useDocumentTitle('工作台')

  const [dashboard, setDashboard] = useState(null)
  const [loading, setLoading] = useState(true)
  const [dashboardError, setDashboardError] = useState('')
  const [modalOpen, setModalOpen] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [modalError, setModalError] = useState('')

  const canCreateKnowledgeBase = canCreateKnowledgeBaseForRole(currentUser?.role)

  const loadDashboard = useCallback(async () => {
    try {
      setLoading(true)
      setDashboardError('')
      const response = await workspaceApi.getCurrentDashboard()
      if (response.code === 200) {
        setDashboard(response.data)
      }
    } catch (error) {
      console.error('加载工作台失败', error)
      setDashboard(null)
      setDashboardError(error?.message || '加载工作台失败，请稍后重试')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadDashboard()
  }, [currentUser?.id, currentUser?.tenantId, loadDashboard])

  useEffect(() => {
    const handleKnowledgeBasesChanged = () => {
      loadDashboard()
    }

    window.addEventListener(KNOWLEDGE_BASES_CHANGED_EVENT, handleKnowledgeBasesChanged)
    return () => {
      window.removeEventListener(KNOWLEDGE_BASES_CHANGED_EVENT, handleKnowledgeBasesChanged)
    }
  }, [loadDashboard])

  const handleSubmitKnowledgeBase = async (formData) => {
    try {
      setSubmitting(true)
      setModalError('')
      const response = await knowledgeBaseApi.createKnowledgeBase({
        ...formData,
        tenantId: currentUser.tenantId,
      })
      const createdKnowledgeBaseId = response?.data?.id
      toast.success(`知识库”${formData.name}”已创建`)
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
    return <DashboardSkeleton />
  }

  if (!dashboard) {
    return (
      <PageState
        eyebrow="工作台不可用"
        title="当前工作区暂时无法打开"
        description={dashboardError || '请稍后重试，或重新登录后再进入工作台。'}
        primaryAction={{ label: '重新加载', onClick: loadDashboard }}
      />
    )
  }

  const knowledgeBases = dashboard.knowledgeBases || []
  const recentDocuments = dashboard.recentDocuments || []
  const knowledgeBaseMap = new Map(knowledgeBases.map((item) => [item.id, item]))
  const summaryCards = [
    {
      label: '最近编辑',
      value: recentDocuments.length,
      hint: recentDocuments[0]
        ? `最后更新 ${dayjs(recentDocuments[0].updatedAt).format('MM-DD HH:mm')}`
        : '从知识库开始写第一篇文档',
    },
    {
      label: '知识库',
      value: knowledgeBases.length,
      hint: knowledgeBases[0]
        ? `最近入口：${knowledgeBases[0].name}`
        : '当前工作区还没有知识库',
    },
  ]

  return (
    <div className={styles.page}>
      <section className={styles.heroPanel}>
        <div className={styles.heroMain}>
          <div className={styles.eyebrow}>工作区</div>
          <h1 className={styles.title}>{dashboard.workspace.name}</h1>
          <div className={styles.heroMeta}>
            <span>{knowledgeBases.length} 个知识库</span>
            <span>{recentDocuments.length} 篇最近文档</span>
            <span>当前角色 {currentUser.role}</span>
          </div>
        </div>
        <div className={styles.workspaceActions}>
          {canCreateKnowledgeBase ? (
            <button type="button" className={styles.primaryButton} onClick={() => setModalOpen(true)}>
              新建知识库
            </button>
          ) : null}
        </div>
      </section>

      <section className={styles.summaryGrid}>
        {summaryCards.map((card) => (
          <article key={card.label} className={styles.summaryCard}>
            <span className={styles.summaryLabel}>{card.label}</span>
            <strong className={styles.summaryValue}>{card.value}</strong>
            <p className={styles.summaryHint}>{card.hint}</p>
          </article>
        ))}
      </section>

      <section className={styles.contentGrid}>
        <div className={styles.mainColumn}>
          <section className={styles.stagePanel}>
            <div className={styles.stageHeader}>
              <div>
                <div className={styles.eyebrow}>最近编辑</div>
                <h2 className={styles.stageTitle}>继续编辑</h2>
              </div>
              <span className={styles.stageMeta}>{recentDocuments.length} 篇</span>
            </div>
            <div className={styles.documentList}>
              {recentDocuments.length > 0 ? (
                recentDocuments.map((document, index) => {
                  const knowledgeBase = knowledgeBaseMap.get(document.knowledgeBaseId)

                  return (
                    <article
                      key={document.id}
                      className={`${styles.documentItem} ${index === 0 ? styles.documentItemActive : ''}`}
                      onClick={() => navigate(`/docs/${document.id}/edit`)}
                    >
                      <div className={styles.documentMark} aria-hidden="true" />
                      <div className={`${styles.itemBody} ${styles.documentBody}`}>
                        <div className={styles.documentTopline}>
                          <div className={styles.itemTitle}>{document.title}</div>
                          <span className={styles.documentAction}>继续编辑</span>
                        </div>
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
                  <p>进入知识库开始写文档</p>
                </div>
              )}
            </div>
          </section>
        </div>

        <aside className={styles.sidebarColumn}>
          <section className={styles.sidebarCard}>
            <div className={styles.sidebarSection}>
              <div className={styles.sidebarHeader}>
                <h2>知识库</h2>
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
                        <div className={styles.itemTitle}>{knowledgeBase.name}</div>
                        <div className={styles.itemMeta}>
                          <span>{knowledgeBase.documentCount} 篇文档</span>
                        </div>
                      </div>
                      <div className={styles.itemTail} aria-hidden="true">›</div>
                    </article>
                  ))
                ) : (
                  <div className={styles.emptyState}>
                    <strong>还没有知识库</strong>
                    <p>创建知识库开始整理文档</p>
                  </div>
                )}
              </div>
            </div>
          </section>
        </aside>
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
