import { useEffect, useMemo } from 'react'
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { DashboardSkeleton } from '../../components/Feedback/Skeleton'
import WorkspaceInviteModal from '../../components/Workspace/WorkspaceInviteModal'
import { useToast } from '../../components/Feedback/Toast'
import { useAuth } from '../../contexts/AuthContext'
import { useWorkspaceManageController } from '../../hooks/useWorkspaceManageController'
import styles from './WorkspaceManage.module.css'

const WorkspaceManageLayout = () => {
  const location = useLocation()
  const navigate = useNavigate()
  const toast = useToast()
  const { currentUser, sessionLoading } = useAuth()
  const controller = useWorkspaceManageController()

  useEffect(() => {
    const feedback = location.state?.feedback
    if (!feedback) {
      return
    }

    if (feedback.type === 'error') {
      toast.error(feedback.message)
    } else {
      toast.success(feedback.message)
    }

    navigate(location.pathname, { replace: true, state: null })
  }, [location.pathname, location.state, navigate, toast])

  const navigationItems = useMemo(() => {
    const items = []

    if (controller.canInviteMembers) {
      items.push({ to: 'members', label: '成员' })
    }
    if (controller.canManageKnowledgeBaseTrash) {
      items.push({ to: 'trash', label: '回收站' })
    }
    if (controller.canManageServiceAccounts) {
      items.push({ to: 'access', label: '开放接入' })
    }
    items.push({ to: 'security', label: '安全与会话' })
    if (controller.canViewWorkspaceAudit) {
      items.push({ to: 'audit', label: '审计' })
    }

    return items
  }, [
    controller.canInviteMembers,
    controller.canManageKnowledgeBaseTrash,
    controller.canManageServiceAccounts,
    controller.canViewWorkspaceAudit,
  ])

  if (sessionLoading) {
    return <DashboardSkeleton />
  }

  return (
    <div className={styles.page}>
      <section className={styles.hero}>
        <p className={styles.eyebrow}>Workspace</p>
        <h1 className={styles.title}>工作区管理</h1>
        <p className={styles.description}>成员、回收站、接入、安全与审计统一在这里处理。</p>
        <div className={styles.metaRow}>
          <span className={styles.metaPill}>{currentUser?.tenantName}</span>
          <span className={styles.metaPill}>{controller.knowledgeBases.length} 个知识库</span>
          <span className={styles.metaPill}>当前角色 {currentUser?.role}</span>
        </div>
        <nav className={styles.tabNav}>
          {navigationItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) => `${styles.tabLink} ${isActive ? styles.tabLinkActive : ''}`}
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
      </section>

      <div className={styles.content}>
        <Outlet context={controller} />
      </div>

      <WorkspaceInviteModal
        open={controller.inviteModalOpen}
        currentRole={controller.currentUser?.role}
        submitting={controller.inviteSubmitting}
        errorMessage={controller.inviteError}
        inviteResult={controller.latestInvite}
        invites={controller.tenantInvites}
        inviteListLoading={controller.inviteListLoading}
        revokingInviteId={controller.revokingInviteId}
        showInviteHistory={false}
        onClose={controller.closeInviteModal}
        onSubmit={controller.handleCreateInvite}
        onRevoke={controller.handleRevokeInvite}
        onRefreshInvites={controller.loadTenantInvites}
      />
    </div>
  )
}

export default WorkspaceManageLayout
