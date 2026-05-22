import { useCallback, useEffect, useMemo, useState } from 'react'
import dayjs from 'dayjs'
import { useNavigate } from 'react-router-dom'
import { useToast } from '../components/Feedback/Toast'
import { useAuth } from '../contexts/AuthContext'
import { auditApi } from '../services/api/auditApi'
import { authApi } from '../services/api/authApi'
import { knowledgeBaseApi } from '../services/api/knowledgeBaseApi'
import { tenantInviteApi } from '../services/api/tenantInviteApi'
import {
  canInviteTenantMembersForRole,
  canManageKnowledgeBaseTrashForRole,
  canManageServiceAccountsForRole,
  canManageTenantSessionsForRole,
  canViewWorkspaceAuditForRole,
} from '../utils/knowledgeBaseAccess'
import { emitKnowledgeBasesChanged, KNOWLEDGE_BASES_CHANGED_EVENT } from '../utils/knowledgeBaseEvents'
import { useKnowledgeBaseNavigation } from './useKnowledgeBaseNavigation'

const INVITE_STATUS = {
  ACTIVE: 1,
  ACCEPTED: 2,
  REVOKED: 3,
}

const AUDIT_STORAGE_SCOPE_LABELS = {
  ACTIVE: '活跃审计',
  ARCHIVED: '归档审计',
}

const SESSION_CLIENT_LABELS = {
  MEMORA_WEB_APP: 'Web 控制台',
  BROWSER_SESSION: '浏览器会话',
  DIRECT_API: '接口调用',
}

const formatSessionTime = (value) => {
  if (!value) {
    return '未知'
  }
  return dayjs(value).format('MM-DD HH:mm')
}

const resolveSessionClientLabel = (session) => {
  const clientType = session?.clientType || ''
  return SESSION_CLIENT_LABELS[clientType] || clientType || '会话'
}

const resolveSessionTitle = (session) => {
  const userAgent = `${session?.userAgent || ''}`.trim()
  if (!userAgent) {
    return resolveSessionClientLabel(session)
  }
  if (userAgent.length <= 72) {
    return userAgent
  }
  return `${userAgent.slice(0, 72)}...`
}

const resolveSessionOwnerLabel = (session) => {
  if (session?.displayName) {
    return session.displayName
  }
  if (session?.username) {
    return session.username
  }
  return session?.userId ? `用户 #${session.userId}` : '未知成员'
}

const buildTenantSessionGroups = (sessions = []) => {
  const groups = []
  const groupMap = new Map()

  sessions.forEach((session) => {
    const groupKey = session?.userId || `unknown-${session?.id}`
    if (!groupMap.has(groupKey)) {
      const nextGroup = {
        userId: session?.userId,
        username: session?.username || '',
        displayName: resolveSessionOwnerLabel(session),
        role: session?.role || '',
        ownedByCurrentUser: Boolean(session?.ownedByCurrentUser),
        sessions: [],
      }
      groupMap.set(groupKey, nextGroup)
      groups.push(nextGroup)
    }

    groupMap.get(groupKey).sessions.push(session)
  })

  return groups
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

export const useWorkspaceManageController = () => {
  const navigate = useNavigate()
  const toast = useToast()
  const { currentUser } = useAuth()
  const { knowledgeBases, refreshKnowledgeBases } = useKnowledgeBaseNavigation(currentUser?.tenantId, {
    errorMessage: '加载工作区知识库失败',
  })

  const canInviteMembers = canInviteTenantMembersForRole(currentUser?.role)
  const canManageKnowledgeBaseTrash = canManageKnowledgeBaseTrashForRole(currentUser?.role)
  const canManageServiceAccounts = canManageServiceAccountsForRole(currentUser?.role)
  const canManageTenantSessions = canManageTenantSessionsForRole(currentUser?.role)
  const canViewWorkspaceAudit = canViewWorkspaceAuditForRole(currentUser?.role)

  const [inviteModalOpen, setInviteModalOpen] = useState(false)
  const [inviteSubmitting, setInviteSubmitting] = useState(false)
  const [inviteError, setInviteError] = useState('')
  const [latestInvite, setLatestInvite] = useState(null)
  const [tenantInvites, setTenantInvites] = useState([])
  const [inviteListLoading, setInviteListLoading] = useState(false)
  const [invitesLoaded, setInvitesLoaded] = useState(false)
  const [revokingInviteId, setRevokingInviteId] = useState(null)

  const [deletedKnowledgeBases, setDeletedKnowledgeBases] = useState([])
  const [knowledgeBaseTrashLoading, setKnowledgeBaseTrashLoading] = useState(false)
  const [knowledgeBaseTrashError, setKnowledgeBaseTrashError] = useState('')
  const [knowledgeBaseTrashLoaded, setKnowledgeBaseTrashLoaded] = useState(false)
  const [restoringKnowledgeBaseId, setRestoringKnowledgeBaseId] = useState(null)

  const [recentAuditEvents, setRecentAuditEvents] = useState([])
  const [auditLoading, setAuditLoading] = useState(false)
  const [auditError, setAuditError] = useState('')
  const [auditSummary, setAuditSummary] = useState(null)
  const [auditSummaryLoading, setAuditSummaryLoading] = useState(false)
  const [auditSummaryLoaded, setAuditSummaryLoaded] = useState(false)
  const [auditLoaded, setAuditLoaded] = useState(false)
  const [auditExportingScope, setAuditExportingScope] = useState('')
  const [auditRetentionRunning, setAuditRetentionRunning] = useState(false)

  const [sessions, setSessions] = useState([])
  const [sessionLoading, setSessionLoading] = useState(false)
  const [sessionsLoaded, setSessionsLoaded] = useState(false)
  const [sessionError, setSessionError] = useState('')
  const [sessionRevokingId, setSessionRevokingId] = useState(null)
  const [sessionRevokingOthers, setSessionRevokingOthers] = useState(false)

  const [tenantSessions, setTenantSessions] = useState([])
  const [tenantSessionLoading, setTenantSessionLoading] = useState(false)
  const [tenantSessionsLoaded, setTenantSessionsLoaded] = useState(false)
  const [tenantSessionError, setTenantSessionError] = useState('')
  const [tenantSessionRevokingId, setTenantSessionRevokingId] = useState(null)
  const [tenantSessionRevokingUserId, setTenantSessionRevokingUserId] = useState(null)

  const loadTenantInvites = useCallback(async () => {
    if (!canInviteMembers) {
      setTenantInvites([])
      setInvitesLoaded(true)
      return []
    }

    try {
      setInviteListLoading(true)
      setInviteError('')
      const response = await tenantInviteApi.listInvites()
      const nextInvites = (response?.data || []).map(decorateInvite)
      setTenantInvites(nextInvites)
      setInvitesLoaded(true)
      return nextInvites
    } catch (error) {
      console.error('加载工作区邀请列表失败', error)
      setTenantInvites([])
      setInviteError(error?.message || '加载邀请列表失败，请稍后重试')
      setInvitesLoaded(true)
      return []
    } finally {
      setInviteListLoading(false)
    }
  }, [canInviteMembers])

  const loadDeletedKnowledgeBases = useCallback(async () => {
    if (!canManageKnowledgeBaseTrash) {
      setDeletedKnowledgeBases([])
      setKnowledgeBaseTrashLoaded(true)
      return []
    }

    try {
      setKnowledgeBaseTrashLoading(true)
      setKnowledgeBaseTrashError('')
      const response = await knowledgeBaseApi.getDeletedKnowledgeBases()
      const nextDeletedKnowledgeBases = response?.code === 200 ? (response.data || []) : []
      setDeletedKnowledgeBases(nextDeletedKnowledgeBases)
      setKnowledgeBaseTrashLoaded(true)
      return nextDeletedKnowledgeBases
    } catch (error) {
      console.error('加载知识库回收站失败', error)
      setDeletedKnowledgeBases([])
      setKnowledgeBaseTrashError(error?.message || '加载知识库回收站失败，请稍后重试')
      setKnowledgeBaseTrashLoaded(true)
      return []
    } finally {
      setKnowledgeBaseTrashLoading(false)
    }
  }, [canManageKnowledgeBaseTrash])

  const loadRecentAuditEvents = useCallback(async () => {
    if (!canViewWorkspaceAudit) {
      setRecentAuditEvents([])
      setAuditLoaded(true)
      setAuditError('')
      return []
    }

    try {
      setAuditLoading(true)
      setAuditError('')
      const response = await auditApi.listAuditLogs({ size: 8 })
      const nextEvents = response?.data?.records || []
      setRecentAuditEvents(nextEvents)
      setAuditLoaded(true)
      return nextEvents
    } catch (error) {
      console.error('加载工作区审计记录失败', error)
      setRecentAuditEvents([])
      setAuditError(error?.message || '加载审计记录失败，请稍后重试')
      setAuditLoaded(true)
      return []
    } finally {
      setAuditLoading(false)
    }
  }, [canViewWorkspaceAudit])

  const loadAuditSummary = useCallback(async () => {
    if (!canViewWorkspaceAudit) {
      setAuditSummary(null)
      setAuditSummaryLoaded(true)
      return null
    }

    try {
      setAuditSummaryLoading(true)
      setAuditError('')
      const response = await auditApi.getAuditSummary()
      const nextSummary = response?.data || null
      setAuditSummary(nextSummary)
      setAuditSummaryLoaded(true)
      return nextSummary
    } catch (error) {
      console.error('加载审计汇总失败', error)
      setAuditSummary(null)
      setAuditError(error?.message || '加载审计汇总失败，请稍后重试')
      setAuditSummaryLoaded(true)
      return null
    } finally {
      setAuditSummaryLoading(false)
    }
  }, [canViewWorkspaceAudit])

  const loadSessions = useCallback(async () => {
    try {
      setSessionLoading(true)
      setSessionError('')
      const response = await authApi.listSessions()
      const nextSessions = response?.code === 200 ? (response.data || []) : []
      setSessions(nextSessions)
      setSessionsLoaded(true)
      return nextSessions
    } catch (error) {
      console.error('加载当前会话列表失败', error)
      setSessions([])
      setSessionError(error?.message || '加载当前会话列表失败，请稍后重试')
      setSessionsLoaded(true)
      return []
    } finally {
      setSessionLoading(false)
    }
  }, [])

  const loadTenantSessions = useCallback(async () => {
    if (!canManageTenantSessions) {
      setTenantSessions([])
      setTenantSessionsLoaded(true)
      setTenantSessionError('')
      return []
    }

    try {
      setTenantSessionLoading(true)
      setTenantSessionError('')
      const response = await authApi.listTenantSessions()
      const nextSessions = response?.code === 200 ? (response.data || []) : []
      setTenantSessions(nextSessions)
      setTenantSessionsLoaded(true)
      return nextSessions
    } catch (error) {
      console.error('加载工作区会话概览失败', error)
      setTenantSessions([])
      setTenantSessionError(error?.message || '加载工作区会话概览失败，请稍后重试')
      setTenantSessionsLoaded(true)
      return []
    } finally {
      setTenantSessionLoading(false)
    }
  }, [canManageTenantSessions])

  const handleRefreshAudit = useCallback(async () => {
    await Promise.all([loadRecentAuditEvents(), loadAuditSummary()])
  }, [loadAuditSummary, loadRecentAuditEvents])

  const handleWorkspaceManageChanged = useCallback(async () => {
    await refreshKnowledgeBases()
    const tasks = []
    if (auditLoaded || canViewWorkspaceAudit) {
      tasks.push(loadRecentAuditEvents())
    }
    if (auditSummaryLoaded || canViewWorkspaceAudit) {
      tasks.push(loadAuditSummary())
    }
    if (tasks.length > 0) {
      await Promise.all(tasks)
    }
  }, [
    auditLoaded,
    auditSummaryLoaded,
    canViewWorkspaceAudit,
    loadAuditSummary,
    loadRecentAuditEvents,
    refreshKnowledgeBases,
  ])

  useEffect(() => {
    const handleKnowledgeBasesChanged = () => {
      refreshKnowledgeBases()
      if (knowledgeBaseTrashLoaded) {
        loadDeletedKnowledgeBases()
      }
      if (auditLoaded) {
        loadRecentAuditEvents()
      }
      if (auditSummaryLoaded) {
        loadAuditSummary()
      }
    }

    window.addEventListener(KNOWLEDGE_BASES_CHANGED_EVENT, handleKnowledgeBasesChanged)
    return () => {
      window.removeEventListener(KNOWLEDGE_BASES_CHANGED_EVENT, handleKnowledgeBasesChanged)
    }
  }, [
    auditLoaded,
    auditSummaryLoaded,
    knowledgeBaseTrashLoaded,
    loadAuditSummary,
    loadDeletedKnowledgeBases,
    loadRecentAuditEvents,
    refreshKnowledgeBases,
  ])

  const openInviteModal = useCallback(async () => {
    if (!canInviteMembers) {
      return
    }

    setInviteError('')
    setLatestInvite(null)
    setInviteModalOpen(true)
    await loadTenantInvites()
  }, [canInviteMembers, loadTenantInvites])

  const closeInviteModal = useCallback(() => {
    setInviteError('')
    setInviteModalOpen(false)
  }, [])

  const handleCreateInvite = useCallback(async (formData) => {
    try {
      setInviteSubmitting(true)
      setInviteError('')
      const response = await tenantInviteApi.createInvite(formData)
      const invite = decorateInvite(response?.data)
      setLatestInvite(invite)
      await Promise.all([loadTenantInvites(), handleRefreshAudit()])
    } catch (error) {
      console.error('创建成员邀请失败', error)
      setInviteError(error?.message || '创建成员邀请失败，请稍后重试')
    } finally {
      setInviteSubmitting(false)
    }
  }, [handleRefreshAudit, loadTenantInvites])

  const handleRevokeInvite = useCallback(async (inviteId) => {
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
      toast.success('邀请已撤销，原邀请链接将不再可用')
      await handleRefreshAudit()
    } catch (error) {
      console.error('撤销邀请失败', error)
      setInviteError(error?.message || '撤销邀请失败，请稍后重试')
    } finally {
      setRevokingInviteId(null)
    }
  }, [handleRefreshAudit, loadTenantInvites, toast])

  const handleRestoreKnowledgeBase = useCallback(async (knowledgeBaseId) => {
    try {
      setRestoringKnowledgeBaseId(knowledgeBaseId)
      setKnowledgeBaseTrashError('')
      await knowledgeBaseApi.restoreKnowledgeBase(knowledgeBaseId)
      await Promise.all([
        refreshKnowledgeBases(),
        loadDeletedKnowledgeBases(),
        handleRefreshAudit(),
      ])
      emitKnowledgeBasesChanged()
      toast.success('知识库已从回收站恢复，可重新进入继续维护')
    } catch (error) {
      console.error('恢复知识库失败', error)
      setKnowledgeBaseTrashError(error?.message || '恢复知识库失败，请稍后重试')
    } finally {
      setRestoringKnowledgeBaseId(null)
    }
  }, [handleRefreshAudit, loadDeletedKnowledgeBases, refreshKnowledgeBases, toast])

  const handleExportAuditLogs = useCallback(async (storageScope = 'ACTIVE') => {
    try {
      setAuditExportingScope(storageScope)
      setAuditError('')
      await auditApi.exportAuditLogs({ storageScope })
      await handleRefreshAudit()
      toast.success(`${AUDIT_STORAGE_SCOPE_LABELS[storageScope] || '审计'} CSV 已开始下载`)
    } catch (error) {
      console.error('导出审计记录失败', error)
      setAuditError(error?.message || '导出审计记录失败，请稍后重试')
    } finally {
      setAuditExportingScope('')
    }
  }, [handleRefreshAudit, toast])

  const handleRunAuditRetention = useCallback(async () => {
    try {
      setAuditRetentionRunning(true)
      setAuditError('')
      const response = await auditApi.runAuditRetention()
      const archivedCount = response?.data?.archivedCount ?? 0
      const remainingPendingArchiveCount = response?.data?.remainingPendingArchiveCount ?? 0
      await handleRefreshAudit()
      toast.success(archivedCount > 0
        ? `已归档 ${archivedCount} 条过期审计记录，剩余待归档 ${remainingPendingArchiveCount} 条`
        : '当前没有需要归档的过期审计记录')
    } catch (error) {
      console.error('执行审计归档失败', error)
      setAuditError(error?.message || '执行审计归档失败，请稍后重试')
    } finally {
      setAuditRetentionRunning(false)
    }
  }, [handleRefreshAudit, toast])

  const handleRevokeSession = useCallback(async (sessionId) => {
    try {
      setSessionRevokingId(sessionId)
      setSessionError('')
      const response = await authApi.revokeSession(sessionId)
      const revokedCurrent = Boolean(response?.data?.currentSessionRevoked)
      await Promise.all([loadSessions(), handleRefreshAudit()])
      if (revokedCurrent) {
        toast.success('当前设备会话已移除，即将返回登录页')
        navigate('/login', { replace: true })
        return
      }
      toast.success('该设备会话已移除')
    } catch (error) {
      console.error('移除会话失败', error)
      setSessionError(error?.message || '移除会话失败，请稍后重试')
    } finally {
      setSessionRevokingId(null)
    }
  }, [handleRefreshAudit, loadSessions, navigate, toast])

  const handleRevokeOtherSessions = useCallback(async () => {
    try {
      setSessionRevokingOthers(true)
      setSessionError('')
      const response = await authApi.revokeOtherSessions()
      const revokedCount = response?.data?.revokedCount ?? 0
      await Promise.all([loadSessions(), handleRefreshAudit()])
      toast.success(revokedCount > 0 ? `已移除 ${revokedCount} 个其他设备会话` : '当前没有其他设备会话需要移除')
    } catch (error) {
      console.error('移除其他设备会话失败', error)
      setSessionError(error?.message || '移除其他设备会话失败，请稍后重试')
    } finally {
      setSessionRevokingOthers(false)
    }
  }, [handleRefreshAudit, loadSessions, toast])

  const handleRevokeTenantSession = useCallback(async (sessionId) => {
    try {
      setTenantSessionRevokingId(sessionId)
      setTenantSessionError('')
      const response = await authApi.revokeTenantSession(sessionId)
      const revokedCurrent = Boolean(response?.data?.currentSessionRevoked)
      await Promise.all([loadSessions(), loadTenantSessions(), handleRefreshAudit()])
      if (revokedCurrent) {
        toast.success('当前设备会话已从工作区概览中移除，即将返回登录页')
        navigate('/login', { replace: true })
        return
      }
      toast.success('工作区会话已移除')
    } catch (error) {
      console.error('移除工作区会话失败', error)
      setTenantSessionError(error?.message || '移除工作区会话失败，请稍后重试')
    } finally {
      setTenantSessionRevokingId(null)
    }
  }, [handleRefreshAudit, loadSessions, loadTenantSessions, navigate, toast])

  const handleRevokeTenantUserSessions = useCallback(async (userId, displayName) => {
    try {
      setTenantSessionRevokingUserId(userId)
      setTenantSessionError('')
      const response = await authApi.revokeTenantUserSessions(userId)
      const revokedCount = response?.data?.revokedCount ?? 0
      const revokedCurrent = Boolean(response?.data?.currentSessionRevoked)
      await Promise.all([loadSessions(), loadTenantSessions(), handleRefreshAudit()])
      if (revokedCurrent) {
        toast.success(`当前设备会话已包含在 ${displayName} 的会话清理中，即将返回登录页`)
        navigate('/login', { replace: true })
        return
      }
      toast.success(revokedCount > 0
        ? `已移除 ${displayName} 的 ${revokedCount} 个活跃会话`
        : `${displayName} 当前没有可移除的活跃会话`)
    } catch (error) {
      console.error('批量移除成员会话失败', error)
      setTenantSessionError(error?.message || '批量移除成员会话失败，请稍后重试')
    } finally {
      setTenantSessionRevokingUserId(null)
    }
  }, [handleRefreshAudit, loadSessions, loadTenantSessions, navigate, toast])

  const tenantSessionGroups = useMemo(() => buildTenantSessionGroups(tenantSessions), [tenantSessions])
  const otherSessionsCount = useMemo(() => sessions.filter((item) => !item?.current).length, [sessions])

  return {
    currentUser,
    knowledgeBases,
    refreshKnowledgeBases,
    canInviteMembers,
    canManageKnowledgeBaseTrash,
    canManageServiceAccounts,
    canManageTenantSessions,
    canViewWorkspaceAudit,
    inviteModalOpen,
    inviteSubmitting,
    inviteError,
    latestInvite,
    tenantInvites,
    inviteListLoading,
    invitesLoaded,
    revokingInviteId,
    openInviteModal,
    closeInviteModal,
    loadTenantInvites,
    handleCreateInvite,
    handleRevokeInvite,
    deletedKnowledgeBases,
    knowledgeBaseTrashLoading,
    knowledgeBaseTrashError,
    knowledgeBaseTrashLoaded,
    restoringKnowledgeBaseId,
    loadDeletedKnowledgeBases,
    handleRestoreKnowledgeBase,
    recentAuditEvents,
    auditLoading,
    auditError,
    auditLoaded,
    auditSummary,
    auditSummaryLoading,
    auditSummaryLoaded,
    auditExportingScope,
    auditRetentionRunning,
    loadRecentAuditEvents,
    loadAuditSummary,
    handleRefreshAudit,
    handleExportAuditLogs,
    handleRunAuditRetention,
    sessions,
    sessionLoading,
    sessionsLoaded,
    sessionError,
    sessionRevokingId,
    sessionRevokingOthers,
    loadSessions,
    handleRevokeSession,
    handleRevokeOtherSessions,
    tenantSessions,
    tenantSessionLoading,
    tenantSessionsLoaded,
    tenantSessionError,
    tenantSessionRevokingId,
    tenantSessionRevokingUserId,
    tenantSessionGroups,
    loadTenantSessions,
    handleRevokeTenantSession,
    handleRevokeTenantUserSessions,
    otherSessionsCount,
    formatSessionTime,
    resolveSessionClientLabel,
    resolveSessionTitle,
    handleWorkspaceManageChanged,
  }
}
