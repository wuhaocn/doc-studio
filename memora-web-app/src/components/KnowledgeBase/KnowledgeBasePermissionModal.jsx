import { useEffect, useState } from 'react'
import { useEscapeKey } from '../../hooks/useEscapeKey'
import styles from './KnowledgeBasePermissionModal.module.css'

const ROLE_OPTIONS = [
  { value: 'OWNER', label: '所有者' },
  { value: 'ADMIN', label: '管理员' },
  { value: 'EDITOR', label: '编辑者' },
  { value: 'VIEWER', label: '只读' },
]

const MANAGE_ROLES = new Set(['OWNER', 'ADMIN'])
const ROLE_HINTS = {
  OWNER: '可读、可写、可管理，通常用于知识库负责人。',
  ADMIN: '可读、可写、可管理，适合知识库协管角色。',
  EDITOR: '可读、可写，不可管理权限配置和知识库删除。',
  VIEWER: '仅可阅读知识库内容，不可修改文档。',
}

const resolveDefaultRole = (member, currentUserId) => {
  if (member.userId === currentUserId) {
    return member.role === 'OWNER' ? 'OWNER' : 'ADMIN'
  }
  if (ROLE_OPTIONS.some((item) => item.value === member.role)) {
    return member.role
  }
  return 'VIEWER'
}

const buildSelectionMap = (members, currentMembers, currentUserId) => {
  const currentMemberMap = new Map(currentMembers.map((item) => [item.userId, item]))

  return members.reduce((result, member) => {
    const existingMember = currentMemberMap.get(member.userId)
    result[member.userId] = existingMember
      ? {
          enabled: true,
          role: existingMember.role,
        }
      : {
          enabled: false,
          role: resolveDefaultRole(member, currentUserId),
        }
    return result
  }, {})
}

const sortMembers = (members, selectionMap, currentUserId) => {
  return [...members].sort((left, right) => {
    if (left.userId === currentUserId) {
      return -1
    }
    if (right.userId === currentUserId) {
      return 1
    }

    const leftEnabled = Boolean(selectionMap[left.userId]?.enabled)
    const rightEnabled = Boolean(selectionMap[right.userId]?.enabled)
    if (leftEnabled !== rightEnabled) {
      return leftEnabled ? -1 : 1
    }

    return (left.displayName || '').localeCompare(right.displayName || '')
  })
}

const buildMemberSignature = (members) => {
  return [...members]
    .sort((left, right) => left.userId - right.userId)
    .map((item) => `${item.userId}:${item.role}`)
    .join('|')
}

const KnowledgeBasePermissionModal = ({
  open,
  loading = false,
  members = [],
  currentMembers = [],
  currentUserId,
  submitting = false,
  errorMessage = '',
  onClose,
  onSubmit,
}) => {
  const [restricted, setRestricted] = useState(false)
  const [selectionMap, setSelectionMap] = useState({})
  const [localError, setLocalError] = useState('')
  const [search, setSearch] = useState('')
  useEscapeKey(open, onClose)

  useEffect(() => {
    if (!open) {
      return
    }

    const nextRestricted = currentMembers.length > 0
    const nextSelectionMap = buildSelectionMap(members, currentMembers, currentUserId)

    if (!nextRestricted && members.length > 0 && nextSelectionMap[currentUserId]) {
      nextSelectionMap[currentUserId] = {
        enabled: true,
        role: resolveDefaultRole(members.find((item) => item.userId === currentUserId) || {}, currentUserId),
      }
    }

    setRestricted(nextRestricted)
    setSelectionMap(nextSelectionMap)
    setLocalError('')
    setSearch('')
  }, [open, members, currentMembers, currentUserId])

  if (!open) {
    return null
  }

  const handleToggleRestricted = (value) => {
    if (value) {
      setSelectionMap((current) => ({
        ...current,
        [currentUserId]: {
          enabled: true,
          role: current[currentUserId]?.role === 'OWNER' ? 'OWNER' : 'ADMIN',
        },
      }))
    }

    setRestricted(value)
    setLocalError('')
  }

  const handleToggleMember = (userId, enabled) => {
    if (restricted && userId === currentUserId) {
      return
    }

    setSelectionMap((current) => ({
      ...current,
      [userId]: {
        ...current[userId],
        enabled,
      },
    }))
    setLocalError('')
  }

  const handleRoleChange = (userId, role) => {
    if (restricted && userId === currentUserId && !MANAGE_ROLES.has(role)) {
      return
    }

    setSelectionMap((current) => ({
      ...current,
      [userId]: {
        ...current[userId],
        role,
      },
    }))
    setLocalError('')
  }

  const selectedMembers = members
    .filter((member) => selectionMap[member.userId]?.enabled)
    .map((member) => ({
      userId: member.userId,
      role: selectionMap[member.userId]?.role || 'VIEWER',
    }))
    .sort((left, right) => left.userId - right.userId)
  const manageCount = selectedMembers.filter((item) => MANAGE_ROLES.has(item.role)).length
  const currentMemberSignature = buildMemberSignature(currentMembers)
  const nextMemberSignature = buildMemberSignature(selectedMembers)
  const hasChanges = restricted ? currentMemberSignature !== nextMemberSignature : currentMembers.length > 0
  const orderedMembers = sortMembers(members, selectionMap, currentUserId).filter((member) => {
    if (!search.trim()) {
      return true
    }

    const keyword = search.toLowerCase()
    return (
      (member.displayName || '').toLowerCase().includes(keyword) ||
      String(member.userId).includes(keyword) ||
      (member.role || '').toLowerCase().includes(keyword)
    )
  })

  const handleSubmit = async (event) => {
    event.preventDefault()

    if (!restricted) {
      await onSubmit([])
      return
    }

    if (selectedMembers.length === 0) {
      setLocalError('启用独立权限时至少需要选择一个成员，或切换为继承租户权限')
      return
    }

    if (!selectedMembers.some((item) => MANAGE_ROLES.has(item.role))) {
      setLocalError('至少保留一个知识库管理员')
      return
    }

    if (!selectedMembers.some((item) => item.userId === currentUserId && MANAGE_ROLES.has(item.role))) {
      setLocalError('必须保留当前用户的知识库管理权限')
      return
    }

    await onSubmit(selectedMembers)
  }

  return (
    <div className={styles.overlay} role="dialog" aria-modal="true">
      <div className={styles.modal}>
        <div className={styles.header}>
          <div>
            <h2 className={styles.title}>权限配置</h2>
          </div>
          <button type="button" className={styles.closeButton} onClick={onClose}>
            关闭
          </button>
        </div>

        <form className={styles.form} onSubmit={handleSubmit}>
          <div className={styles.summaryBar}>
            <span>{restricted ? '独立知识库权限' : '继承租户权限'}</span>
            <span>{restricted ? `已选 ${selectedMembers.length} 人 / 管理员 ${manageCount} 人` : '当前未限制成员名单'}</span>
          </div>

          <div className={styles.changeBanner}>
            {hasChanges
              ? (restricted ? '当前有未保存的权限变更。' : '保存后将清空独立成员名单，恢复继承租户权限。')
              : '当前配置与服务端保持一致。'}
          </div>

          <div className={styles.modeCard}>
            <label className={styles.modeOption}>
              <input
                type="radio"
                name="permission-mode"
                checked={!restricted}
                onChange={() => handleToggleRestricted(false)}
              />
              <div>
                <strong>继承租户权限</strong>
                <p>沿用租户成员角色，不对当前知识库设置独立成员名单。</p>
              </div>
            </label>
            <label className={styles.modeOption}>
              <input
                type="radio"
                name="permission-mode"
                checked={restricted}
                onChange={() => handleToggleRestricted(true)}
              />
              <div>
                <strong>独立知识库权限</strong>
                <p>仅名单内成员可访问当前知识库，并按知识库角色控制读写和管理。</p>
              </div>
            </label>
          </div>

          <div className={styles.roleGrid}>
            {ROLE_OPTIONS.map((option) => (
              <article key={option.value} className={styles.roleCard}>
                <strong>{option.label}</strong>
                <p>{ROLE_HINTS[option.value]}</p>
              </article>
            ))}
          </div>

          <label className={styles.searchField}>
            <span>成员筛选</span>
            <input
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              placeholder="按姓名、用户 ID 或租户角色筛选"
            />
          </label>

          {loading ? (
            <div className={styles.state}>正在加载成员与权限配置...</div>
          ) : (
            <div className={styles.memberList}>
              {orderedMembers.map((member) => {
                const selection = selectionMap[member.userId] || { enabled: false, role: 'VIEWER' }
                const isCurrentUser = member.userId === currentUserId
                return (
                  <div key={member.userId} className={styles.memberRow}>
                    <label className={styles.memberInfo}>
                      <input
                        type="checkbox"
                        checked={selection.enabled}
                        disabled={!restricted || isCurrentUser}
                        onChange={(event) => handleToggleMember(member.userId, event.target.checked)}
                      />
                      <div>
                        <strong>
                          {member.displayName}
                          {isCurrentUser ? <span className={styles.selfTag}>当前用户</span> : null}
                        </strong>
                        <p>租户角色：{member.role}</p>
                      </div>
                    </label>
                    <select
                      className={styles.roleSelect}
                      value={selection.role}
                      disabled={!restricted || !selection.enabled}
                      onChange={(event) => handleRoleChange(member.userId, event.target.value)}
                    >
                      {ROLE_OPTIONS.filter((option) => !isCurrentUser || MANAGE_ROLES.has(option.value)).map((option) => (
                        <option key={option.value} value={option.value}>
                          {option.label}
                        </option>
                      ))}
                    </select>
                  </div>
                )
              })}
              {!loading && orderedMembers.length === 0 ? (
                <div className={styles.state}>当前筛选条件下没有匹配成员。</div>
              ) : null}
            </div>
          )}

          {(localError || errorMessage) && (
            <div className={styles.errorMessage}>{localError || errorMessage}</div>
          )}

          <div className={styles.footer}>
            <button type="button" className={styles.secondaryButton} onClick={onClose}>
              取消
            </button>
            <button type="submit" className={styles.primaryButton} disabled={submitting || loading || !hasChanges}>
              {submitting ? '保存中...' : hasChanges ? '保存权限' : '未修改'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

export default KnowledgeBasePermissionModal
