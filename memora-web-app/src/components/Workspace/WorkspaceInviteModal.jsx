import { useEffect, useMemo, useState } from 'react'
import { useEscapeKey } from '../../hooks/useEscapeKey'
import styles from './WorkspaceInviteModal.module.css'

const OWNER_ROLE_OPTIONS = [
  { value: 'OWNER', label: '所有者' },
  { value: 'ADMIN', label: '管理员' },
  { value: 'EDITOR', label: '编辑者' },
  { value: 'VIEWER', label: '只读' },
]

const ADMIN_ROLE_OPTIONS = [
  { value: 'ADMIN', label: '管理员' },
  { value: 'EDITOR', label: '编辑者' },
  { value: 'VIEWER', label: '只读' },
]

const DEFAULT_FORM = {
  email: '',
  displayName: '',
  role: 'EDITOR',
  expiresInDays: 7,
}

const WorkspaceInviteModal = ({
  open,
  currentRole,
  submitting = false,
  errorMessage = '',
  inviteResult = null,
  invites = [],
  inviteListLoading = false,
  revokingInviteId = null,
  onClose,
  onSubmit,
  onRevoke,
  onRefreshInvites,
}) => {
  const [form, setForm] = useState(DEFAULT_FORM)
  useEscapeKey(open, onClose)
  const [localError, setLocalError] = useState('')
  const roleOptions = useMemo(() => (currentRole === 'OWNER' ? OWNER_ROLE_OPTIONS : ADMIN_ROLE_OPTIONS), [currentRole])

  useEffect(() => {
    if (!open) {
      return
    }

    setLocalError('')
    setForm({
      ...DEFAULT_FORM,
      role: roleOptions.find((item) => item.value === 'EDITOR')?.value || roleOptions[0]?.value || 'EDITOR',
    })
  }, [open, roleOptions])

  if (!open) {
    return null
  }

  const handleSubmit = async (event) => {
    event.preventDefault()

    if (!form.email.trim()) {
      setLocalError('受邀邮箱不能为空')
      return
    }

    await onSubmit({
      email: form.email.trim(),
      displayName: form.displayName.trim() || undefined,
      role: form.role,
      expiresInDays: Number(form.expiresInDays) || 7,
    })
  }

  const handleCopyInviteLink = async (inviteLink) => {
    if (!inviteLink || !navigator.clipboard) {
      return
    }

    try {
      await navigator.clipboard.writeText(inviteLink)
      setLocalError('')
    } catch (error) {
      setLocalError('复制邀请链接失败，请手动复制')
    }
  }

  return (
    <div className={styles.overlay} role="dialog" aria-modal="true">
      <div className={styles.modal}>
        <div className={styles.header}>
          <div>
            <p className={styles.eyebrow}>成员邀请</p>
            <h2 className={styles.title}>邀请成员加入当前工作区</h2>
            <p className={styles.description}>当前版本使用邀请链接完成加入，不开放公共注册入口。</p>
          </div>
          <button type="button" className={styles.closeButton} onClick={onClose}>
            关闭
          </button>
        </div>

        <form className={styles.form} onSubmit={handleSubmit}>
          <label className={styles.field}>
            <span>受邀邮箱</span>
            <input
              value={form.email}
              onChange={(event) => setForm((current) => ({ ...current, email: event.target.value }))}
              placeholder="例如：editor@company.com"
              required
            />
          </label>

          <div className={styles.grid}>
            <label className={styles.field}>
              <span>成员姓名</span>
              <input
                value={form.displayName}
                onChange={(event) => setForm((current) => ({ ...current, displayName: event.target.value }))}
                placeholder="可选，成员接受邀请时可再调整"
              />
            </label>
            <label className={styles.field}>
              <span>角色</span>
              <select
                value={form.role}
                onChange={(event) => setForm((current) => ({ ...current, role: event.target.value }))}
              >
                {roleOptions.map((option) => (
                  <option key={option.value} value={option.value}>{option.label}</option>
                ))}
              </select>
            </label>
          </div>

          <label className={styles.field}>
            <span>有效期（天）</span>
            <input
              type="number"
              min="1"
              max="30"
              value={form.expiresInDays}
              onChange={(event) => setForm((current) => ({ ...current, expiresInDays: event.target.value }))}
            />
          </label>

          {(localError || errorMessage) && (
            <div className={styles.errorMessage}>{localError || errorMessage}</div>
          )}

          {inviteResult ? (
            <div className={styles.inviteCard}>
              <div className={styles.inviteMeta}>
                <strong>{inviteResult.inviteeEmail}</strong>
                <span>{inviteResult.role} · 截止 {inviteResult.expiresAtText}</span>
              </div>
              {inviteResult.inviteLink ? (
                <>
                  <textarea value={inviteResult.inviteLink} readOnly rows={3} />
                  <div className={styles.inviteActions}>
                    <button
                      type="button"
                      className={styles.secondaryButton}
                      onClick={() => handleCopyInviteLink(inviteResult.inviteLink)}
                    >
                      复制邀请链接
                    </button>
                  </div>
                </>
              ) : (
                <p className={styles.description}>该邀请链接只会在创建当下展示一次；若已丢失，请撤销后重新生成。</p>
              )}
            </div>
          ) : null}

          <section className={styles.inviteHistory}>
            <div className={styles.inviteHistoryHeader}>
              <div>
                <strong>最近邀请</strong>
                <span>这里只保留邀请状态；原始链接只在创建当下展示，丢失后需撤销重建。</span>
              </div>
              <button
                type="button"
                className={styles.secondaryButton}
                onClick={onRefreshInvites}
                disabled={inviteListLoading}
              >
                {inviteListLoading ? '刷新中...' : '刷新列表'}
              </button>
            </div>

            {inviteListLoading ? (
              <div className={styles.inviteHistoryEmpty}>正在加载邀请列表...</div>
            ) : invites.length > 0 ? (
              <div className={styles.inviteHistoryList}>
                {invites.map((invite) => (
                  <article key={invite.id} className={styles.inviteHistoryItem}>
                    <div className={styles.inviteHistoryMain}>
                      <div className={styles.inviteHistoryTopline}>
                        <strong>{invite.inviteeEmail}</strong>
                        <span className={styles.inviteStatus}>{invite.statusText}</span>
                      </div>
                      <div className={styles.inviteHistoryMeta}>
                        <span>{invite.role}</span>
                        <span>创建于 {invite.createdAtText}</span>
                        <span>截止 {invite.expiresAtText}</span>
                      </div>
                    </div>
                    <div className={styles.inviteHistoryActions}>
                      {invite.canRevoke ? (
                        <button
                          type="button"
                          className={styles.secondaryButton}
                          onClick={() => onRevoke(invite.id)}
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
              <div className={styles.inviteHistoryEmpty}>当前还没有可管理的邀请记录。</div>
            )}
          </section>

          <div className={styles.footer}>
            <button type="button" className={styles.secondaryButton} onClick={onClose}>
              取消
            </button>
            <button type="submit" className={styles.primaryButton} disabled={submitting}>
              {submitting ? '生成中...' : '生成邀请链接'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

export default WorkspaceInviteModal
