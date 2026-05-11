import { useCallback, useEffect, useState } from 'react'
import dayjs from 'dayjs'
import { useLocation, useNavigate } from 'react-router-dom'
import AuditEventList from '../../components/Audit/AuditEventList'
import PageState from '../../components/Feedback/PageState'
import TrashListModal from '../../components/KnowledgeBase/TrashListModal'
import ServiceAccountModal from '../../components/Workspace/ServiceAccountModal'
import WorkspaceInviteModal from '../../components/Workspace/WorkspaceInviteModal'
import { useAuth } from '../../contexts/AuthContext'
import KnowledgeBaseFormModal from '../../components/KnowledgeBase/KnowledgeBaseFormModal'
import { auditApi } from '../../services/api/auditApi'
import { knowledgeBaseApi } from '../../services/api/knowledgeBaseApi'
import { tenantInviteApi } from '../../services/api/tenantInviteApi'
import { workspaceApi } from '../../services/api/workspaceApi'
import {
  canCreateKnowledgeBaseForRole,
  canInviteTenantMembersForRole,
  canManageServiceAccountsForRole,
  canManageKnowledgeBaseTrashForRole,
  canViewWorkspaceAuditForRole,
  shouldAutoOpenKnowledgeBaseTrash,
} from '../../utils/knowledgeBaseAccess'
import { emitKnowledgeBasesChanged, KNOWLEDGE_BASES_CHANGED_EVENT } from '../../utils/knowledgeBaseEvents'
import styles from './Home.module.css'

const INVITE_STATUS = {
  ACTIVE: 1,
  ACCEPTED: 2,
  REVOKED: 3,
}

const AUDIT_STORAGE_SCOPE_LABELS = {
  ACTIVE: '活跃审计',
  ARCHIVED: '归档审计',
}

const decorateInvite = (invite) => {
  if (!invite) {
    return null
  }

  const expired = invite.status === INVITE_STATUS.ACTIVE && invite.expiresAt && dayjs(invite.expiresAt).isBefore(dayjs())
  const statusText = invite.status === INVITE_STATUS.ACCEPTED
    ? '已接受'
    : invite.status === INVITE_STATUS.REVOKED
      ? '已撤销'
      : expired
        ? '已过期'
        : '待加入'

  return {
    ...invite,
    expired,
    statusText,
    canRevoke: invite.status === INVITE_STATUS.ACTIVE && !expired,
    inviteLink: invite.inviteToken ? `${window.location.origin}/accept-invite?token=${invite.inviteToken}` : '',
    expiresAtText: invite.expiresAt ? dayjs(invite.expiresAt).format('YYYY-MM-DD HH:mm') : '未设置',
    createdAtText: invite.createdAt ? dayjs(invite.createdAt).format('YYYY-MM-DD HH:mm') : '刚刚',
  }
}

const Home = () => {
  const navigate = useNavigate()
  const location = useLocation()
  const { currentUser } = useAuth()
  const [dashboard, setDashboard] = useState(null)
  const [loading, setLoading] = useState(true)
  const [dashboardError, setDashboardError] = useState('')
  const [modalOpen, setModalOpen] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [modalError, setModalError] = useState('')
  const [feedback, setFeedback] = useState(null)
  const [knowledgeBaseTrashOpen, setKnowledgeBaseTrashOpen] = useState(false)
  const [deletedKnowledgeBases, setDeletedKnowledgeBases] = useState([])
  const [knowledgeBaseTrashLoading, setKnowledgeBaseTrashLoading] = useState(false)
  const [knowledgeBaseTrashError, setKnowledgeBaseTrashError] = useState('')
  const [restoringKnowledgeBaseId, setRestoringKnowledgeBaseId] = useState(null)
  const [inviteModalOpen, setInviteModalOpen] = useState(false)
  const [inviteSubmitting, setInviteSubmitting] = useState(false)
  const [inviteError, setInviteError] = useState('')
  const [latestInvite, setLatestInvite] = useState(null)
  const [tenantInvites, setTenantInvites] = useState([])
  const [inviteListLoading, setInviteListLoading] = useState(false)
  const [revokingInviteId, setRevokingInviteId] = useState(null)
  const [recentAuditEvents, setRecentAuditEvents] = useState([])
  const [auditLoading, setAuditLoading] = useState(false)
  const [auditError, setAuditError] = useState('')
  const [auditSummary, setAuditSummary] = useState(null)
  const [auditSummaryLoading, setAuditSummaryLoading] = useState(false)
  const [auditExportingScope, setAuditExportingScope] = useState('')
  const [auditRetentionRunning, setAuditRetentionRunning] = useState(false)
  const [serviceAccountModalOpen, setServiceAccountModalOpen] = useState(false)
  const canCreateKnowledgeBase = canCreateKnowledgeBaseForRole(currentUser?.role)
  const canInviteMembers = canInviteTenantMembersForRole(currentUser?.role)
  const canManageKnowledgeBaseTrash = canManageKnowledgeBaseTrashForRole(currentUser?.role)
  const canViewWorkspaceAudit = canViewWorkspaceAuditForRole(currentUser?.role)
  const canManageServiceAccounts = canManageServiceAccountsForRole(currentUser?.role)

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

  const loadDeletedKnowledgeBases = useCallback(async () => {
    try {
      setKnowledgeBaseTrashLoading(true)
      setKnowledgeBaseTrashError('')
      const response = await knowledgeBaseApi.getDeletedKnowledgeBases()
      if (response.code === 200) {
        setDeletedKnowledgeBases(response.data || [])
      }
    } catch (error) {
      console.error('加载知识库回收站失败', error)
      setKnowledgeBaseTrashError(error?.message || '加载知识库回收站失败，请稍后重试')
    } finally {
      setKnowledgeBaseTrashLoading(false)
    }
  }, [])

  const openKnowledgeBaseTrash = useCallback(async () => {
    if (!canManageKnowledgeBaseTrash) {
      return
    }

    setKnowledgeBaseTrashOpen(true)
    await loadDeletedKnowledgeBases()
  }, [canManageKnowledgeBaseTrash, loadDeletedKnowledgeBases])

  const loadTenantInvites = useCallback(async () => {
    try {
      setInviteListLoading(true)
      const response = await tenantInviteApi.listInvites()
      const nextInvites = (response?.data || []).map(decorateInvite)
      setTenantInvites(nextInvites)
      return nextInvites
    } catch (error) {
      console.error('加载工作区邀请列表失败', error)
      setInviteError(error?.message || '加载邀请列表失败，请稍后重试')
      return []
    } finally {
      setInviteListLoading(false)
    }
  }, [])

  const loadRecentAuditEvents = useCallback(async () => {
    if (!canViewWorkspaceAudit) {
      setRecentAuditEvents([])
      setAuditError('')
      return []
    }

    try {
      setAuditLoading(true)
      setAuditError('')
      const response = await auditApi.listAuditLogs({ size: 8 })
      const nextEvents = response?.data?.records || []
      setRecentAuditEvents(nextEvents)
      return nextEvents
    } catch (error) {
      console.error('加载工作区审计记录失败', error)
      setRecentAuditEvents([])
      setAuditError(error?.message || '加载审计记录失败，请稍后重试')
      return []
    } finally {
      setAuditLoading(false)
    }
  }, [canViewWorkspaceAudit])

  const loadAuditSummary = useCallback(async () => {
    if (!canViewWorkspaceAudit) {
      setAuditSummary(null)
      return null
    }

    try {
      setAuditSummaryLoading(true)
      const response = await auditApi.getAuditSummary()
      const nextSummary = response?.data || null
      setAuditSummary(nextSummary)
      return nextSummary
    } catch (error) {
      console.error('加载审计汇总失败', error)
      setAuditSummary(null)
      setAuditError(error?.message || '加载审计汇总失败，请稍后重试')
      return null
    } finally {
      setAuditSummaryLoading(false)
    }
  }, [canViewWorkspaceAudit])

  useEffect(() => {
    loadDashboard()
  }, [currentUser.id, currentUser.tenantId, loadDashboard])

  useEffect(() => {
    loadRecentAuditEvents()
  }, [currentUser.id, currentUser.tenantId, loadRecentAuditEvents])

  useEffect(() => {
    loadAuditSummary()
  }, [currentUser.id, currentUser.tenantId, loadAuditSummary])

  useEffect(() => {
    const routeState = location.state || {}
    if (!routeState.feedback && !routeState.openKnowledgeBaseTrash) {
      return
    }

    if (routeState.feedback) {
      setFeedback(routeState.feedback)
    }

    if (shouldAutoOpenKnowledgeBaseTrash(routeState, currentUser?.role)) {
      openKnowledgeBaseTrash()
    }

    navigate(location.pathname, { replace: true, state: null })
  }, [currentUser?.role, location.pathname, location.state, navigate, openKnowledgeBaseTrash])

  useEffect(() => {
    const handleKnowledgeBasesChanged = () => {
      loadDashboard()
      loadAuditSummary()
      if (knowledgeBaseTrashOpen) {
        loadDeletedKnowledgeBases()
      }
    }

    window.addEventListener(KNOWLEDGE_BASES_CHANGED_EVENT, handleKnowledgeBasesChanged)
    return () => {
      window.removeEventListener(KNOWLEDGE_BASES_CHANGED_EVENT, handleKnowledgeBasesChanged)
    }
  }, [knowledgeBaseTrashOpen, loadAuditSummary, loadDashboard, loadDeletedKnowledgeBases])

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
      await loadRecentAuditEvents()
      await loadAuditSummary()
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

  const handleRestoreKnowledgeBase = async (knowledgeBaseId) => {
    try {
      setRestoringKnowledgeBaseId(knowledgeBaseId)
      setKnowledgeBaseTrashError('')
      await knowledgeBaseApi.restoreKnowledgeBase(knowledgeBaseId)
      await Promise.all([
        loadDashboard(),
        loadDeletedKnowledgeBases(),
        loadRecentAuditEvents(),
        loadAuditSummary(),
      ])
      emitKnowledgeBasesChanged()
      setFeedback({
        type: 'success',
        message: '知识库已从回收站恢复，可重新进入继续维护',
      })
    } catch (error) {
      console.error('恢复知识库失败', error)
      setKnowledgeBaseTrashError(error?.message || '恢复知识库失败，请稍后重试')
    } finally {
      setRestoringKnowledgeBaseId(null)
    }
  }

  const handleCreateInvite = async (formData) => {
    try {
      setInviteSubmitting(true)
      setInviteError('')
      const response = await tenantInviteApi.createInvite(formData)
      const invite = decorateInvite(response?.data)
      setLatestInvite(invite)
      await loadTenantInvites()
      await loadRecentAuditEvents()
      await loadAuditSummary()
    } catch (error) {
      console.error('创建成员邀请失败', error)
      setInviteError(error?.message || '创建成员邀请失败，请稍后重试')
    } finally {
      setInviteSubmitting(false)
    }
  }

  const handleRevokeInvite = async (inviteId) => {
    try {
      setRevokingInviteId(inviteId)
      setInviteError('')
      await tenantInviteApi.revokeInvite(inviteId)
      const nextInvites = await loadTenantInvites()
      setLatestInvite((current) => {
        if (!current || current.id !== inviteId) {
          return current
        }
        return nextInvites.find((item) => item.id === inviteId) || current
      })
      setFeedback({ type: 'success', message: '邀请已撤销，原邀请链接将不再可用' })
      await loadRecentAuditEvents()
      await loadAuditSummary()
    } catch (error) {
      console.error('撤销邀请失败', error)
      setInviteError(error?.message || '撤销邀请失败，请稍后重试')
    } finally {
      setRevokingInviteId(null)
    }
  }

  if (loading) {
    return <div className={styles.state}>正在加载工作台...</div>
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

  const handleExportAuditLogs = async (storageScope = 'ACTIVE') => {
    try {
      setAuditExportingScope(storageScope)
      setAuditError('')
      await auditApi.exportAuditLogs({ storageScope })
      await Promise.all([loadRecentAuditEvents(), loadAuditSummary()])
      setFeedback({
        type: 'success',
        message: `${AUDIT_STORAGE_SCOPE_LABELS[storageScope] || '审计'} CSV 已开始下载`,
      })
    } catch (error) {
      console.error('导出审计记录失败', error)
      setAuditError(error?.message || '导出审计记录失败，请稍后重试')
    } finally {
      setAuditExportingScope('')
    }
  }

  const handleRunAuditRetention = async () => {
    try {
      setAuditRetentionRunning(true)
      setAuditError('')
      const response = await auditApi.runAuditRetention()
      const archivedCount = response?.data?.archivedCount ?? 0
      const remainingPendingArchiveCount = response?.data?.remainingPendingArchiveCount ?? 0
      await Promise.all([loadRecentAuditEvents(), loadAuditSummary()])
      setFeedback({
        type: 'success',
        message: archivedCount > 0
          ? `已归档 ${archivedCount} 条过期审计记录，剩余待归档 ${remainingPendingArchiveCount} 条`
          : '当前没有需要归档的过期审计记录',
      })
    } catch (error) {
      console.error('执行审计归档失败', error)
      setAuditError(error?.message || '执行审计归档失败，请稍后重试')
    } finally {
      setAuditRetentionRunning(false)
    }
  }

  return (
    <div className={styles.page}>
      {feedback && (
        <div className={`${styles.feedback} ${feedback.type === 'error' ? styles.feedbackError : styles.feedbackSuccess}`}>
          {feedback.message}
        </div>
      )}

      <section className={styles.contentGrid}>
        <aside className={styles.sidebarColumn}>
          <section className={styles.sidebarCard}>
            <div className={styles.workspaceSection}>
              <div className={styles.eyebrow}>当前工作区</div>
              <h1 className={styles.title}>{dashboard.workspace.name}</h1>
              <div className={styles.workspaceMeta}>{knowledgeBases.length} 个知识库</div>
              <div className={styles.workspaceActions}>
                {canCreateKnowledgeBase ? (
                  <button type="button" className={styles.primaryButton} onClick={() => setModalOpen(true)}>
                    新建知识库
                  </button>
                ) : null}
                {canInviteMembers ? (
                  <button
                    type="button"
                    className={styles.secondaryButton}
                    onClick={() => {
                      setInviteError('')
                      setLatestInvite(null)
                      setInviteModalOpen(true)
                      void loadTenantInvites()
                    }}
                  >
                    邀请成员
                  </button>
                ) : null}
                {canManageKnowledgeBaseTrash ? (
                  <button type="button" className={styles.secondaryButton} onClick={openKnowledgeBaseTrash}>
                    知识库回收站
                  </button>
                ) : null}
                {canManageServiceAccounts ? (
                  <button type="button" className={styles.secondaryButton} onClick={() => setServiceAccountModalOpen(true)}>
                    API key 管理
                  </button>
                ) : null}
              </div>
            </div>

            <div className={styles.sidebarSection}>
              <div className={styles.sidebarHeader}>
                <h2>知识库列表</h2>
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
                    <p>先创建一个知识库，再开始整理文档。</p>
                  </div>
                )}
              </div>
            </div>
          </section>
        </aside>

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
                  <p>进入知识库后开始写第一篇文档。</p>
                </div>
              )}
            </div>
          </section>

          {canViewWorkspaceAudit ? (
            <section className={styles.auditPanel}>
              <div className={styles.stageHeader}>
                <div>
                  <div className={styles.eyebrow}>治理与追踪</div>
                  <h2 className={styles.stageTitle}>最近治理记录</h2>
                </div>
                <div className={styles.auditHeaderActions}>
                  <span className={styles.stageMeta}>{recentAuditEvents.length} 条</span>
                  <div className={styles.auditActionButtons}>
                    <button
                      type="button"
                      className={styles.secondaryButton}
                      disabled={Boolean(auditExportingScope) || auditRetentionRunning}
                      onClick={handleRunAuditRetention}
                    >
                      {auditRetentionRunning ? '归档中...' : '执行归档'}
                    </button>
                    <button
                      type="button"
                      className={styles.secondaryButton}
                      disabled={Boolean(auditExportingScope) || auditRetentionRunning}
                      onClick={() => handleExportAuditLogs('ACTIVE')}
                    >
                      {auditExportingScope === 'ACTIVE' ? '导出中...' : '导出活跃 CSV'}
                    </button>
                    <button
                      type="button"
                      className={styles.secondaryButton}
                      disabled={Boolean(auditExportingScope) || auditRetentionRunning}
                      onClick={() => handleExportAuditLogs('ARCHIVED')}
                    >
                      {auditExportingScope === 'ARCHIVED' ? '导出中...' : '导出归档 CSV'}
                    </button>
                  </div>
                </div>
              </div>
              <div className={styles.auditSummaryGrid}>
                <article className={styles.auditSummaryCard}>
                  <span>总记录</span>
                  <strong>{auditSummaryLoading ? '...' : auditSummary?.totalCount ?? '-'}</strong>
                </article>
                <article className={styles.auditSummaryCard}>
                  <span>活跃记录</span>
                  <strong>{auditSummaryLoading ? '...' : auditSummary?.activeCount ?? '-'}</strong>
                </article>
                <article className={styles.auditSummaryCard}>
                  <span>归档记录</span>
                  <strong>{auditSummaryLoading ? '...' : auditSummary?.archivedCount ?? '-'}</strong>
                </article>
                <article className={styles.auditSummaryCard}>
                  <span>失败记录</span>
                  <strong>{auditSummaryLoading ? '...' : auditSummary?.failureCount ?? '-'}</strong>
                </article>
                <article className={styles.auditSummaryCard}>
                  <span>待归档</span>
                  <strong>{auditSummaryLoading ? '...' : auditSummary?.pendingArchiveCount ?? '-'}</strong>
                </article>
                <article className={styles.auditSummaryCard}>
                  <span>归档阈值</span>
                  <strong>{auditSummaryLoading ? '...' : `${auditSummary?.configuredRetentionDays ?? '-'} 天`}</strong>
                </article>
              </div>
              <div className={styles.auditGovernanceMeta}>
                <span>
                  最近归档：
                  {auditSummary?.lastArchivedAt ? dayjs(auditSummary.lastArchivedAt).format('YYYY-MM-DD HH:mm') : '未执行'}
                </span>
                <span>单次批量：{auditSummary?.archiveBatchSize ?? '-'}</span>
                <span>导出上限：{auditSummary?.exportMaxSize ?? '-'}</span>
              </div>
              <AuditEventList
                events={recentAuditEvents}
                loading={auditLoading}
                errorMessage={auditError}
                emptyTitle="当前工作区还没有关键治理记录"
                emptyDescription="登录、邀请、知识库变更、受控分享和 Open API 写操作会显示在这里。"
                showKnowledgeBaseName
              />
            </section>
          ) : null}
        </div>
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

      <TrashListModal
        open={knowledgeBaseTrashOpen}
        eyebrow="知识库回收站"
        title={dashboard.workspace.name}
        description="这里保留当前工作区已删除的知识库。恢复后会重新回到知识库列表，并继续沿用原有权限边界。"
        items={deletedKnowledgeBases}
        loading={knowledgeBaseTrashLoading}
        errorMessage={knowledgeBaseTrashError}
        restoringItemId={restoringKnowledgeBaseId}
        emptyTitle="当前工作区回收站为空"
        emptyDescription="删除后的知识库会暂存到这里，便于继续恢复文档主流程。"
        onClose={() => {
          setKnowledgeBaseTrashError('')
          setKnowledgeBaseTrashOpen(false)
        }}
        onRestore={(item) => handleRestoreKnowledgeBase(item.id)}
        getItemTitle={(item) => item.name}
        getItemDescription={(item) => item.description || `标识：${item.slug || '未设置'}`}
        getItemMeta={(item) => [
          `${item.documentCount || 0} 篇文档`,
          item.deletedAt ? `删除于 ${dayjs(item.deletedAt).format('MM-DD HH:mm')}` : '',
          item.deletedBy ? `删除人 #${item.deletedBy}` : '',
        ]}
      />

      <WorkspaceInviteModal
        open={inviteModalOpen}
        currentRole={currentUser?.role}
        submitting={inviteSubmitting}
        errorMessage={inviteError}
        inviteResult={latestInvite}
        invites={tenantInvites}
        inviteListLoading={inviteListLoading}
        revokingInviteId={revokingInviteId}
        onClose={() => {
          setInviteError('')
          setInviteModalOpen(false)
        }}
        onSubmit={handleCreateInvite}
        onRevoke={handleRevokeInvite}
        onRefreshInvites={loadTenantInvites}
      />

      <ServiceAccountModal
        open={serviceAccountModalOpen}
        knowledgeBases={knowledgeBases}
        onClose={() => setServiceAccountModalOpen(false)}
        onChanged={async () => {
          await loadRecentAuditEvents()
          await loadAuditSummary()
        }}
      />
    </div>
  )
}

export default Home
