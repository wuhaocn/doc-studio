import { useEffect } from 'react'
import { useOutletContext } from 'react-router-dom'
import PageState from '../../components/Feedback/PageState'
import { useDocumentTitle } from '../../hooks/useDocumentTitle'
import { copyText } from '../../utils/copyText'
import styles from './WorkspaceManage.module.css'

const WorkspaceManageMembersPage = () => {
  const {
    canInviteMembers,
    invitesLoaded,
    inviteListLoading,
    tenantInvites,
    latestInvite,
    inviteError,
    revokingInviteId,
    openInviteModal,
    loadTenantInvites,
    handleRevokeInvite,
  } = useOutletContext()
  useDocumentTitle('工作区成员')

  useEffect(() => {
    if (canInviteMembers && !invitesLoaded) {
      void loadTenantInvites()
    }
  }, [canInviteMembers, invitesLoaded, loadTenantInvites])

  if (!canInviteMembers) {
    return (
      <PageState
        title="当前角色不能管理成员邀请"
        description="仅 OWNER / ADMIN 可管理。"
      />
    )
  }

  const activeInviteCount = tenantInvites.filter((item) => item.canRevoke).length
  const acceptedInviteCount = tenantInvites.filter((item) => item.statusText === '已接受').length
  const expiredInviteCount = tenantInvites.filter((item) => item.statusText === '已过期').length

  return (
    <div className={styles.stack}>
      <section className={styles.surfaceCard}>
        <div className={styles.sectionHeader}>
          <div>
            <h2 className={styles.sectionTitle}>成员邀请</h2>
            <span className={styles.sectionMeta}>签发邀请并查看最近记录。</span>
          </div>
          <div className={styles.sectionActions}>
            <button type="button" className={styles.secondaryButton} onClick={loadTenantInvites} disabled={inviteListLoading}>
              {inviteListLoading ? '刷新中...' : '刷新'}
            </button>
            <button type="button" className={styles.primaryButton} onClick={openInviteModal}>
              邀请成员
            </button>
          </div>
        </div>

        <div className={styles.metricsGrid}>
          <article className={styles.metricCard}>
            <span>待加入</span>
            <strong>{activeInviteCount}</strong>
          </article>
          <article className={styles.metricCard}>
            <span>已接受</span>
            <strong>{acceptedInviteCount}</strong>
          </article>
          <article className={styles.metricCard}>
            <span>已过期</span>
            <strong>{expiredInviteCount}</strong>
          </article>
        </div>

        {latestInvite?.inviteLink ? (
          <div className={styles.calloutCard}>
            <div className={styles.itemTopline}>
              <strong className={styles.itemTitle}>{latestInvite.inviteeEmail}</strong>
              <span className={styles.statusBadge}>{latestInvite.statusText}</span>
            </div>
            <div className={styles.itemMeta}>
              <span>{latestInvite.role}</span>
              <span>截止 {latestInvite.expiresAtText}</span>
            </div>
            <div className={styles.calloutActions}>
              <button
                type="button"
                className={styles.secondaryButton}
                onClick={() => copyText(latestInvite.inviteLink)}
              >
                复制最近邀请链接
              </button>
            </div>
          </div>
        ) : null}

        {inviteError ? <div className={styles.errorState}>{inviteError}</div> : null}

        {inviteListLoading && tenantInvites.length === 0 ? (
          <div className={styles.emptyState}>
            <strong>正在加载邀请记录</strong>
          </div>
        ) : tenantInvites.length > 0 ? (
          <div className={styles.list}>
            {tenantInvites.map((invite) => (
              <article key={invite.id} className={styles.itemCard}>
                <div className={styles.itemMain}>
                  <div className={styles.itemTopline}>
                    <strong className={styles.itemTitle}>{invite.inviteeEmail}</strong>
                    <span className={styles.statusBadge}>{invite.statusText}</span>
                  </div>
                  <div className={styles.itemMeta}>
                    <span>{invite.role}</span>
                    <span>创建于 {invite.createdAtText}</span>
                    <span>截止 {invite.expiresAtText}</span>
                  </div>
                </div>
                <div className={styles.sectionActions}>
                  {invite.inviteLink ? (
                    <button
                      type="button"
                      className={styles.secondaryButton}
                      onClick={() => copyText(invite.inviteLink)}
                    >
                      复制链接
                    </button>
                  ) : null}
                  {invite.canRevoke ? (
                    <button
                      type="button"
                      className={styles.dangerButton}
                      onClick={() => handleRevokeInvite(invite.id)}
                      disabled={revokingInviteId === invite.id}
                    >
                      {revokingInviteId === invite.id ? '撤销中...' : '撤销'}
                    </button>
                  ) : null}
                </div>
              </article>
            ))}
          </div>
        ) : (
          <div className={styles.emptyState}>
            <strong>当前还没有邀请记录</strong>
          </div>
        )}
      </section>
    </div>
  )
}

export default WorkspaceManageMembersPage
