import { useEffect } from 'react'
import { useOutletContext } from 'react-router-dom'
import { useDocumentTitle } from '../../hooks/useDocumentTitle'
import styles from './WorkspaceManage.module.css'

const WorkspaceManageSecurityPage = () => {
  const {
    canManageTenantSessions,
    sessions,
    sessionLoading,
    sessionsLoaded,
    sessionError,
    sessionRevokingId,
    sessionRevokingOthers,
    loadSessions,
    handleRevokeSession,
    handleRevokeOtherSessions,
    otherSessionsCount,
    tenantSessionLoading,
    tenantSessionsLoaded,
    tenantSessionError,
    tenantSessionRevokingId,
    tenantSessionRevokingUserId,
    tenantSessionGroups,
    loadTenantSessions,
    handleRevokeTenantSession,
    handleRevokeTenantUserSessions,
    resolveSessionTitle,
    resolveSessionClientLabel,
    formatSessionTime,
  } = useOutletContext()
  useDocumentTitle('安全与会话')

  useEffect(() => {
    if (!sessionsLoaded) {
      void loadSessions()
    }
  }, [loadSessions, sessionsLoaded])

  useEffect(() => {
    if (canManageTenantSessions && !tenantSessionsLoaded) {
      void loadTenantSessions()
    }
  }, [canManageTenantSessions, loadTenantSessions, tenantSessionsLoaded])

  return (
    <div className={styles.stack}>
      <section className={styles.surfaceCard}>
        <div className={styles.sectionHeader}>
          <div>
            <h2 className={styles.sectionTitle}>当前登录设备</h2>
            <span className={styles.sectionMeta}>管理当前账号的浏览器与接口会话。</span>
          </div>
          <div className={styles.sectionActions}>
            <button
              type="button"
              className={styles.secondaryButton}
              disabled={sessionLoading || sessionRevokingOthers || Boolean(sessionRevokingId)}
              onClick={loadSessions}
            >
              {sessionLoading ? '刷新中...' : '刷新'}
            </button>
            <button
              type="button"
              className={styles.secondaryButton}
              disabled={sessionLoading || sessionRevokingOthers || Boolean(sessionRevokingId) || otherSessionsCount === 0}
              onClick={handleRevokeOtherSessions}
            >
              {sessionRevokingOthers ? '移除中...' : '退出其他设备'}
            </button>
          </div>
        </div>

        {sessionError ? <div className={styles.errorState}>{sessionError}</div> : null}

        {sessionLoading && sessions.length === 0 ? (
          <div className={styles.emptyState}>
            <strong>正在加载会话</strong>
          </div>
        ) : sessions.length > 0 ? (
          <div className={styles.sessionList}>
            {sessions.map((session) => (
              <article key={session.id} className={styles.sessionItem}>
                <div className={styles.itemMain}>
                  <div className={styles.sessionTopline}>
                    <strong className={styles.itemTitle}>{resolveSessionTitle(session)}</strong>
                    {session.current ? <span className={styles.currentBadge}>当前设备</span> : null}
                  </div>
                  <div className={styles.itemMeta}>
                    <span>{resolveSessionClientLabel(session)}</span>
                    {session.ipAddress ? <span>{session.ipAddress}</span> : null}
                    <span>最近活跃 {formatSessionTime(session.lastActiveAt)}</span>
                    <span>过期于 {formatSessionTime(session.expiresAt)}</span>
                  </div>
                </div>
                {session.current ? null : (
                  <div className={styles.sectionActions}>
                    <button
                      type="button"
                      className={styles.dangerButton}
                      disabled={sessionRevokingOthers || sessionRevokingId === session.id}
                      onClick={() => handleRevokeSession(session.id)}
                    >
                      {sessionRevokingId === session.id ? '移除中...' : '移除'}
                    </button>
                  </div>
                )}
              </article>
            ))}
          </div>
        ) : (
          <div className={styles.emptyState}>
            <strong>当前没有活跃会话</strong>
          </div>
        )}
      </section>

      {canManageTenantSessions ? (
        <section className={styles.surfaceCard}>
          <div className={styles.sectionHeader}>
            <div>
              <h2 className={styles.sectionTitle}>工作区设备概览</h2>
              <span className={styles.sectionMeta}>按成员查看和清理工作区会话。</span>
            </div>
            <div className={styles.sectionActions}>
              <button
                type="button"
                className={styles.secondaryButton}
                disabled={tenantSessionLoading || Boolean(tenantSessionRevokingId) || Boolean(tenantSessionRevokingUserId)}
                onClick={loadTenantSessions}
              >
                {tenantSessionLoading ? '刷新中...' : '刷新'}
              </button>
            </div>
          </div>

          {tenantSessionError ? <div className={styles.errorState}>{tenantSessionError}</div> : null}

          {tenantSessionLoading && tenantSessionGroups.length === 0 ? (
            <div className={styles.emptyState}>
              <strong>正在加载工作区会话</strong>
            </div>
          ) : tenantSessionGroups.length > 0 ? (
            <div className={styles.sessionGroupList}>
              {tenantSessionGroups.map((group) => (
                <section key={group.userId || group.displayName} className={styles.sessionGroup}>
                  <div className={styles.sessionGroupHeader}>
                    <div className={styles.itemMain}>
                      <div className={styles.sessionOwnerLine}>
                        <strong className={styles.itemTitle}>{group.displayName}</strong>
                        {group.role ? <span className={styles.sessionRoleBadge}>{group.role}</span> : null}
                        {group.ownedByCurrentUser ? <span className={styles.currentBadge}>当前用户</span> : null}
                      </div>
                      <div className={styles.itemMeta}>
                        <span>{group.username ? `@${group.username}` : `用户 #${group.userId}`}</span>
                        <span>{group.sessions.length} 个活跃会话</span>
                      </div>
                    </div>
                    {group.ownedByCurrentUser ? null : (
                      <div className={styles.sectionActions}>
                        <button
                          type="button"
                          className={styles.secondaryButton}
                          disabled={tenantSessionLoading || Boolean(tenantSessionRevokingId) || tenantSessionRevokingUserId === group.userId}
                          onClick={() => handleRevokeTenantUserSessions(group.userId, group.displayName)}
                        >
                          {tenantSessionRevokingUserId === group.userId ? '移除中...' : '移除该成员全部会话'}
                        </button>
                      </div>
                    )}
                  </div>

                  <div className={styles.sessionList}>
                    {group.sessions.map((session) => (
                      <article key={session.id} className={styles.sessionItem}>
                        <div className={styles.itemMain}>
                          <div className={styles.sessionTopline}>
                            <strong className={styles.itemTitle}>{resolveSessionTitle(session)}</strong>
                            {session.current ? <span className={styles.currentBadge}>当前设备</span> : null}
                          </div>
                          <div className={styles.itemMeta}>
                            <span>{resolveSessionClientLabel(session)}</span>
                            {session.ipAddress ? <span>{session.ipAddress}</span> : null}
                            <span>最近活跃 {formatSessionTime(session.lastActiveAt)}</span>
                            <span>过期于 {formatSessionTime(session.expiresAt)}</span>
                          </div>
                        </div>
                        {session.current ? null : (
                          <div className={styles.sectionActions}>
                            <button
                              type="button"
                              className={styles.dangerButton}
                              disabled={tenantSessionRevokingUserId === group.userId || tenantSessionRevokingId === session.id}
                              onClick={() => handleRevokeTenantSession(session.id)}
                            >
                              {tenantSessionRevokingId === session.id ? '移除中...' : '移除'}
                            </button>
                          </div>
                        )}
                      </article>
                    ))}
                  </div>
                </section>
              ))}
            </div>
          ) : (
            <div className={styles.emptyState}>
              <strong>当前没有工作区活跃会话</strong>
            </div>
          )}
        </section>
      ) : null}
    </div>
  )
}

export default WorkspaceManageSecurityPage
