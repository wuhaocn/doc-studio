import { useCallback, useEffect, useMemo, useState } from 'react'
import dayjs from 'dayjs'
import { useConfirm } from '../Feedback/ConfirmDialog'
import { serviceAccountApi } from '../../services/api/serviceAccountApi'
import { copyText } from '../../utils/copyText'
import { countManagedApiKeys, resolveManagedApiKeyDisplay } from '../../utils/workspaceAccess'
import styles from './ServiceAccessPanel.module.css'

const DEFAULT_EXPIRE_DAYS = 90

const SERVICE_ACCOUNT_STATUS_LABELS = {
  0: '已停用',
  1: '生效中',
}

const API_KEY_STATUS_LABELS = {
  0: '已禁用',
  1: '生效中',
  2: '已删除',
}

const ACCESS_MODE_LABELS = {
  WRITE: '可写',
  READ: '只读',
}

const buildScopeDraft = (knowledgeBases = [], scopes = []) => {
  if (scopes.length > 0) {
    return Object.fromEntries(
      scopes.map((scope) => [scope.knowledgeBaseId, scope.accessMode || 'WRITE']),
    )
  }

  if (knowledgeBases[0]?.id) {
    return {
      [knowledgeBases[0].id]: 'WRITE',
    }
  }

  return {}
}

const buildCreateAccountForm = (knowledgeBases = []) => ({
  name: '',
  description: '',
  keyName: '默认写入密钥',
  expiresInDays: DEFAULT_EXPIRE_DAYS,
  scopeDrafts: buildScopeDraft(knowledgeBases),
})

const buildCreateKeyForm = (knowledgeBases = [], scopes = [], name = '新增密钥') => ({
  name,
  expiresInDays: DEFAULT_EXPIRE_DAYS,
  scopeDrafts: buildScopeDraft(knowledgeBases, scopes),
})

const normalizeScopesPayload = (scopeDrafts = {}) => {
  return Object.entries(scopeDrafts).map(([knowledgeBaseId, accessMode]) => ({
    knowledgeBaseId: Number(knowledgeBaseId),
    accessMode,
  }))
}

const ScopePicker = ({
  knowledgeBases = [],
  scopeDrafts = {},
  onToggle,
  onModeChange,
  emptyMessage,
}) => {
  if (knowledgeBases.length === 0) {
    return <div className={styles.emptyState}>{emptyMessage}</div>
  }

  return (
    <div className={styles.scopeGrid}>
      {knowledgeBases.map((knowledgeBase) => {
        const checked = !!scopeDrafts[knowledgeBase.id]
        return (
          <label
            key={knowledgeBase.id}
            className={`${styles.scopeItem} ${checked ? styles.scopeItemChecked : ''}`}
          >
            <div className={styles.scopeTopline}>
              <input
                type="checkbox"
                checked={checked}
                onChange={() => onToggle(knowledgeBase.id)}
              />
              <div>
                <strong>{knowledgeBase.name}</strong>
                <span>{knowledgeBase.documentCount || 0} 篇文档</span>
              </div>
            </div>

            {checked ? (
              <div className={styles.scopeModeRow}>
                <span>方式</span>
                <select
                  value={scopeDrafts[knowledgeBase.id]}
                  onChange={(event) => onModeChange(knowledgeBase.id, event.target.value)}
                >
                  <option value="WRITE">可写</option>
                  <option value="READ">只读</option>
                </select>
              </div>
            ) : (
              <div className={styles.subtleText}>未开放</div>
            )}
          </label>
        )
      })}
    </div>
  )
}

const ServiceAccessPanel = ({
  knowledgeBases = [],
  tenantId,
  onChanged,
}) => {
  const confirm = useConfirm()
  const [serviceAccounts, setServiceAccounts] = useState([])
  const [loading, setLoading] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [actionKey, setActionKey] = useState('')
  const [errorMessage, setErrorMessage] = useState('')
  const [feedback, setFeedback] = useState('')
  const [showCreateForm, setShowCreateForm] = useState(false)
  const [accountForm, setAccountForm] = useState(() => buildCreateAccountForm(knowledgeBases))
  const [createKeyAccountId, setCreateKeyAccountId] = useState(null)
  const [createKeyForm, setCreateKeyForm] = useState(() => buildCreateKeyForm(knowledgeBases))
  const [editingScopeKeyId, setEditingScopeKeyId] = useState(null)
  const [scopeEditDrafts, setScopeEditDrafts] = useState({})

  const knowledgeBaseNameMap = useMemo(() => {
    return new Map(knowledgeBases.map((item) => [item.id, item.name]))
  }, [knowledgeBases])
  const totalApiKeyCount = useMemo(() => {
    return countManagedApiKeys(serviceAccounts)
  }, [serviceAccounts])

  const loadServiceAccounts = useCallback(async () => {
    if (!tenantId) {
      setServiceAccounts([])
      return []
    }

    try {
      setLoading(true)
      const response = await serviceAccountApi.listServiceAccounts()
      const nextAccounts = response?.data || []
      setServiceAccounts(nextAccounts)
      if (nextAccounts.length === 0) {
        setShowCreateForm(true)
      }
      return nextAccounts
    } catch (error) {
      console.error('加载 service account 失败', error)
      setServiceAccounts([])
      setErrorMessage(error?.message || '加载接入凭证失败，请稍后重试')
      return []
    } finally {
      setLoading(false)
    }
  }, [tenantId])

  useEffect(() => {
    if (!tenantId) {
      setServiceAccounts([])
      return
    }

    setAccountForm(buildCreateAccountForm(knowledgeBases))
    setCreateKeyForm(buildCreateKeyForm(knowledgeBases))
    setCreateKeyAccountId(null)
    setEditingScopeKeyId(null)
    setScopeEditDrafts({})
    setErrorMessage('')
    setFeedback('')
    void loadServiceAccounts()
  }, [knowledgeBases, loadServiceAccounts, tenantId])

  const toggleScope = (setter, knowledgeBaseId) => {
    setter((current) => {
      const nextScopeDrafts = { ...(current.scopeDrafts || {}) }
      if (nextScopeDrafts[knowledgeBaseId]) {
        delete nextScopeDrafts[knowledgeBaseId]
      } else {
        nextScopeDrafts[knowledgeBaseId] = 'WRITE'
      }
      return {
        ...current,
        scopeDrafts: nextScopeDrafts,
      }
    })
  }

  const changeScopeMode = (setter, knowledgeBaseId, accessMode) => {
    setter((current) => ({
      ...current,
      scopeDrafts: {
        ...(current.scopeDrafts || {}),
        [knowledgeBaseId]: accessMode,
      },
    }))
  }

  const scopeCount = (scopeDrafts) => Object.keys(scopeDrafts || {}).length

  const runManagedAction = async (loadingId, action, successMessage) => {
    try {
      setActionKey(loadingId)
      setErrorMessage('')
      setFeedback('')
      await action()
      await loadServiceAccounts()
      await onChanged?.()
      setFeedback(successMessage)
    } catch (error) {
      console.error('执行 service account 操作失败', error)
      setErrorMessage(error?.message || '执行接入管理操作失败，请稍后重试')
    } finally {
      setActionKey('')
    }
  }

  const handleCreateServiceAccount = async (event) => {
    event.preventDefault()

    try {
      setSubmitting(true)
      setErrorMessage('')
      setFeedback('')
      await serviceAccountApi.createServiceAccount({
        name: accountForm.name.trim(),
        description: accountForm.description.trim() || undefined,
        keyName: accountForm.keyName.trim(),
        expiresInDays: Number(accountForm.expiresInDays) || DEFAULT_EXPIRE_DAYS,
        scopes: normalizeScopesPayload(accountForm.scopeDrafts),
      })
      setFeedback('主体和首把密钥已创建')
      await loadServiceAccounts()
      await onChanged?.()
      setAccountForm(buildCreateAccountForm(knowledgeBases))
      setShowCreateForm(false)
    } catch (error) {
      console.error('创建 service account 失败', error)
      setErrorMessage(error?.message || '创建机器主体失败，请稍后重试')
    } finally {
      setSubmitting(false)
    }
  }

  const handleCopyApiKey = async (plainTextKey) => {
    if (!plainTextKey) {
      return
    }

    try {
      await copyText(plainTextKey)
      setFeedback('密钥已复制')
    } catch (error) {
      console.error('复制密钥失败', error)
      setErrorMessage('复制失败，请手动保存')
    }
  }

  const handleToggleCreateForm = () => {
    const nextShowCreateForm = !showCreateForm
    setShowCreateForm(nextShowCreateForm)
    if (nextShowCreateForm) {
      setAccountForm(buildCreateAccountForm(knowledgeBases))
    }
    setCreateKeyAccountId(null)
    setEditingScopeKeyId(null)
    setErrorMessage('')
    setFeedback('')
  }

  const handleToggleServiceAccount = async (account) => {
    const enabling = account.status !== 1
    const confirmed = await confirm({
      title: enabling ? '恢复主体' : '停用主体',
      description: enabling
        ? '恢复后，主体下可用的密钥会重新生效。'
        : '停用后，这个主体下的密钥会立即停止使用。',
      confirmLabel: enabling ? '确认恢复' : '确认停用',
      danger: !enabling,
    })
    if (!confirmed) {
      return
    }

    await runManagedAction(
      `service-account:${account.id}:${enabling ? 'enable' : 'disable'}`,
      () => (enabling
        ? serviceAccountApi.enableServiceAccount(account.id)
        : serviceAccountApi.disableServiceAccount(account.id)),
      enabling ? '主体已恢复使用' : '主体已停用',
    )
  }

  const handleOpenCreateKeyForm = (account) => {
    const seedScopes = account.apiKeys?.[0]?.scopes || []
    setCreateKeyAccountId((current) => (current === account.id ? null : account.id))
    setEditingScopeKeyId(null)
    setCreateKeyForm(buildCreateKeyForm(
      knowledgeBases,
      seedScopes,
      `${account.name} 扩展密钥`,
    ))
    setErrorMessage('')
    setFeedback('')
  }

  const handleSubmitCreateKey = async (event, accountId) => {
    event.preventDefault()

    try {
      setSubmitting(true)
      setErrorMessage('')
      setFeedback('')
      await serviceAccountApi.createApiKey(accountId, {
        name: createKeyForm.name.trim(),
        expiresInDays: Number(createKeyForm.expiresInDays) || DEFAULT_EXPIRE_DAYS,
        scopes: normalizeScopesPayload(createKeyForm.scopeDrafts),
      })
      setFeedback('新密钥已创建')
      setCreateKeyAccountId(null)
      setCreateKeyForm(buildCreateKeyForm(knowledgeBases))
      await loadServiceAccounts()
      await onChanged?.()
    } catch (error) {
      console.error('新增密钥失败', error)
      setErrorMessage(error?.message || '新增密钥失败，请稍后重试')
    } finally {
      setSubmitting(false)
    }
  }

  const handleOpenScopeEditor = (apiKey) => {
    setEditingScopeKeyId((current) => (current === apiKey.id ? null : apiKey.id))
    setCreateKeyAccountId(null)
    setScopeEditDrafts(buildScopeDraft(knowledgeBases, apiKey.scopes || []))
    setErrorMessage('')
    setFeedback('')
  }

  const handleSubmitScopeUpdate = async (event, apiKeyId) => {
    event.preventDefault()
    await runManagedAction(
      `api-key:${apiKeyId}:scope`,
      () => serviceAccountApi.updateApiKeyScope(apiKeyId, {
        scopes: normalizeScopesPayload(scopeEditDrafts),
      }),
      '访问范围已更新',
    )
    setEditingScopeKeyId(null)
    setScopeEditDrafts({})
  }

  const handleRotateApiKey = async (apiKey) => {
    const confirmed = await confirm({
      title: '重新生成密钥',
      description: '重新生成后，旧密钥会立即失效，请确认调用方已经准备好切换。',
      confirmLabel: '确认重新生成',
      danger: true,
    })
    if (!confirmed) {
      return
    }

    await runManagedAction(
      `api-key:${apiKey.id}:rotate`,
      () => serviceAccountApi.rotateApiKey(apiKey.id, {}),
      '新密钥已生成，旧密钥已失效',
    )
  }

  const handleDisableApiKey = async (apiKey) => {
    const confirmed = await confirm({
      title: '暂停密钥',
      description: '暂停后，这把密钥将无法继续使用。',
      confirmLabel: '确认暂停',
      danger: true,
    })
    if (!confirmed) {
      return
    }

    await runManagedAction(
      `api-key:${apiKey.id}:disable`,
      () => serviceAccountApi.disableApiKey(apiKey.id),
      '密钥已暂停',
    )
  }

  const handleRevokeApiKey = async (apiKey) => {
    const confirmed = await confirm({
      title: '删除密钥',
      description: '删除后，这把密钥会立即失效，并从当前列表移除；相关审计记录会继续保留。',
      confirmLabel: '确认删除',
      danger: true,
    })
    if (!confirmed) {
      return
    }

    await runManagedAction(
      `api-key:${apiKey.id}:revoke`,
      () => serviceAccountApi.revokeApiKey(apiKey.id),
      '密钥已删除',
    )
  }

  return (
    <section className={styles.panel}>
      <div className={styles.header}>
        <div>
          <p className={styles.eyebrow}>开放接入</p>
          <h2 className={styles.title}>机器接入凭证</h2>
          <p className={styles.description}>集中管理机器主体、可访问知识库和访问密钥，进入页面即可查看仍可展示的当前密钥。</p>
        </div>
        <div className={styles.headerTools}>
          <div className={styles.headerStats}>
            <div className={styles.headerStat}>
              <strong>{serviceAccounts.length}</strong>
              <span>已签发主体</span>
            </div>
            <div className={styles.headerStat}>
              <strong>{totalApiKeyCount}</strong>
              <span>当前密钥</span>
            </div>
            <div className={styles.headerStat}>
              <strong>{knowledgeBases.length}</strong>
              <span>可授权知识库</span>
            </div>
          </div>
          <button type="button" className={styles.primaryButton} onClick={handleToggleCreateForm}>
            {showCreateForm ? '收起创建' : '新建主体'}
          </button>
        </div>
      </div>

      {(errorMessage || feedback) ? (
        <div className={`${styles.message} ${errorMessage ? styles.messageError : styles.messageSuccess}`}>
          {errorMessage || feedback}
        </div>
      ) : null}

      <section className={styles.listSection}>
        <div className={styles.listHeader}>
          <strong>机器主体</strong>
          <span>{serviceAccounts.length} 个</span>
        </div>

        {loading ? (
          <div className={styles.emptyState}>正在加载接入凭证...</div>
        ) : serviceAccounts.length > 0 ? (
          <div className={styles.accountList}>
            {serviceAccounts.map((account) => {
              const accountStatusLabel = SERVICE_ACCOUNT_STATUS_LABELS[account.status] || '未知状态'
              const accountActionLoading = actionKey === `service-account:${account.id}:${account.status === 1 ? 'disable' : 'enable'}`
              const accountSeedScopes = account.apiKeys?.[0]?.scopes || []

              return (
                <article key={account.id} className={styles.accountCard}>
                  <div className={styles.accountHeader}>
                    <div>
                      <div className={styles.accountTitleRow}>
                        <strong>{account.name}</strong>
                        <span className={`${styles.statusBadge} ${account.status === 1 ? '' : styles.statusBadgeMuted}`}>
                          {accountStatusLabel}
                        </span>
                      </div>
                      <div className={styles.accountMeta}>
                        <span>创建于 {account.createdAt ? dayjs(account.createdAt).format('YYYY-MM-DD HH:mm') : '刚刚'}</span>
                        <span>{account.apiKeys?.length || 0} 把密钥</span>
                      </div>
                      {account.description ? <p className={styles.accountDescription}>{account.description}</p> : null}
                      {account.status !== 1 ? (
                        <div className={styles.accountStateNote}>
                          主体已停用，相关密钥暂不可使用。
                        </div>
                      ) : null}
                    </div>

                    <div className={styles.accountActions}>
                      <button
                        type="button"
                        className={styles.secondaryButton}
                        onClick={() => handleOpenCreateKeyForm(account)}
                      >
                        {createKeyAccountId === account.id ? '收起新增密钥' : '新增密钥'}
                      </button>
                      <button
                        type="button"
                        className={account.status === 1 ? styles.dangerButton : styles.secondaryButton}
                        disabled={accountActionLoading}
                        onClick={() => handleToggleServiceAccount(account)}
                      >
                        {accountActionLoading
                          ? (account.status === 1 ? '停用中...' : '启用中...')
                          : (account.status === 1 ? '停用主体' : '恢复主体')}
                      </button>
                    </div>
                  </div>

                  {createKeyAccountId === account.id ? (
                    <form className={styles.inlineForm} onSubmit={(event) => handleSubmitCreateKey(event, account.id)}>
                      <div className={styles.formGrid}>
                        <label className={styles.field}>
                          <span>新密钥名称</span>
                          <input
                            value={createKeyForm.name}
                            onChange={(event) => setCreateKeyForm((current) => ({ ...current, name: event.target.value }))}
                            placeholder="例如：数据同步密钥"
                            required
                          />
                        </label>
                        <label className={styles.field}>
                          <span>有效期（天）</span>
                          <input
                            type="number"
                            min="1"
                            max="3650"
                            value={createKeyForm.expiresInDays}
                            onChange={(event) => setCreateKeyForm((current) => ({ ...current, expiresInDays: event.target.value }))}
                          />
                        </label>
                      </div>
                      <div className={styles.field}>
                        <span>新密钥可访问的知识库</span>
                        <ScopePicker
                          knowledgeBases={knowledgeBases}
                          scopeDrafts={createKeyForm.scopeDrafts}
                          onToggle={(knowledgeBaseId) => toggleScope(setCreateKeyForm, knowledgeBaseId)}
                          onModeChange={(knowledgeBaseId, accessMode) => changeScopeMode(setCreateKeyForm, knowledgeBaseId, accessMode)}
                          emptyMessage="当前工作区还没有知识库，暂时无法新增密钥。"
                        />
                      </div>
                      <div className={styles.inlineFormFooter}>
                        <div className={styles.keyActions}>
                          <button
                            type="button"
                            className={styles.secondaryButton}
                            onClick={() => {
                              setCreateKeyAccountId(null)
                              setCreateKeyForm(buildCreateKeyForm(knowledgeBases, accountSeedScopes))
                            }}
                          >
                            取消
                          </button>
                          <button
                            type="submit"
                            className={styles.primaryButton}
                            disabled={submitting || scopeCount(createKeyForm.scopeDrafts) === 0}
                          >
                            {submitting ? '创建中...' : '创建密钥'}
                          </button>
                        </div>
                      </div>
                    </form>
                  ) : null}

                  <div className={styles.keyList}>
                    {(account.apiKeys || []).length > 0 ? (account.apiKeys || []).map((apiKey) => {
                      const statusLabel = apiKey.expired
                        ? '已过期'
                        : API_KEY_STATUS_LABELS[apiKey.status] || '未知状态'
                      const scopes = apiKey.scopes || []
                      const scopeSummary = scopes.map((scope) => {
                        const knowledgeBaseName = knowledgeBaseNameMap.get(scope.knowledgeBaseId) || `知识库 #${scope.knowledgeBaseId}`
                        return `${knowledgeBaseName} · ${ACCESS_MODE_LABELS[scope.accessMode] || scope.accessMode}`
                      })
                      const keyLoadingPrefix = `api-key:${apiKey.id}`
                      const canEditKey = apiKey.status !== 2
                      const { plainTextKey, hint: secretHint, state: secretState } = resolveManagedApiKeyDisplay(apiKey)

                      return (
                        <section key={apiKey.id} className={styles.keyCard}>
                          <div className={styles.keyTopline}>
                            <div>
                              <div className={styles.accountTitleRow}>
                                <strong>{apiKey.name}</strong>
                                <span className={`${styles.statusBadge} ${apiKey.status === 1 && !apiKey.expired ? '' : styles.statusBadgeMuted}`}>
                                  {statusLabel}
                                </span>
                              </div>
                              <div className={styles.keyMeta}>
                                <span>{apiKey.keyPrefix}</span>
                                <span>{apiKey.createdAt ? `创建于 ${dayjs(apiKey.createdAt).format('MM-DD HH:mm')}` : '刚刚'}</span>
                                <span>{apiKey.lastUsedAt ? `最近使用 ${dayjs(apiKey.lastUsedAt).format('MM-DD HH:mm')}` : '尚未调用'}</span>
                              </div>
                            </div>

                            <div className={styles.keyActions}>
                              {canEditKey ? (
                                <button
                                  type="button"
                                  className={styles.secondaryButton}
                                  onClick={() => handleOpenScopeEditor(apiKey)}
                                >
                                  {editingScopeKeyId === apiKey.id ? '收起访问范围' : '访问范围'}
                                </button>
                              ) : null}
                              {canEditKey ? (
                                <button
                                  type="button"
                                  className={styles.secondaryButton}
                                  disabled={actionKey === `${keyLoadingPrefix}:rotate`}
                                  onClick={() => handleRotateApiKey(apiKey)}
                                >
                                  {actionKey === `${keyLoadingPrefix}:rotate`
                                    ? '生成中...'
                                    : '重新生成'}
                                </button>
                              ) : null}
                              {apiKey.status === 1 ? (
                                <button
                                  type="button"
                                  className={styles.secondaryButton}
                                  disabled={actionKey === `${keyLoadingPrefix}:disable`}
                                  onClick={() => handleDisableApiKey(apiKey)}
                                >
                                  {actionKey === `${keyLoadingPrefix}:disable` ? '暂停中...' : '暂停'}
                                </button>
                              ) : null}
                              {apiKey.status !== 2 ? (
                                <button
                                  type="button"
                                  className={styles.dangerButton}
                                  disabled={actionKey === `${keyLoadingPrefix}:revoke`}
                                  onClick={() => handleRevokeApiKey(apiKey)}
                                >
                                  {actionKey === `${keyLoadingPrefix}:revoke` ? '删除中...' : '删除'}
                                </button>
                              ) : null}
                            </div>
                          </div>

                          <div className={styles.scopeSummaryList}>
                            {scopeSummary.length > 0 ? scopeSummary.map((item) => (
                              <span key={`${apiKey.id}:${item}`} className={styles.scopeSummaryItem}>{item}</span>
                            )) : (
                              <span className={styles.scopeSummaryItem}>未配置权限</span>
                            )}
                          </div>

                          {plainTextKey ? (
                            <div className={styles.secretCard}>
                              <div className={styles.secretCardHeader}>
                                <strong>当前密钥</strong>
                                <span>{secretState === 'visible' ? '进入页面直接可见' : '可复制保存'}</span>
                              </div>
                              <textarea value={plainTextKey} readOnly rows={2} />
                              <div className={styles.secretActions}>
                                <button
                                  type="button"
                                  className={styles.secondaryButton}
                                  onClick={() => handleCopyApiKey(plainTextKey)}
                                >
                                  复制密钥
                                </button>
                              </div>
                            </div>
                          ) : null}

                          {!plainTextKey ? (
                            <div className={styles.secretHint}>{secretHint}</div>
                          ) : null}

                          <div className={styles.scopeMeta}>
                            <span>到期</span>
                            <strong>{apiKey.expiresAt ? dayjs(apiKey.expiresAt).format('YYYY-MM-DD HH:mm') : '未设置'}</strong>
                          </div>

                          {editingScopeKeyId === apiKey.id ? (
                            <form className={styles.inlineForm} onSubmit={(event) => handleSubmitScopeUpdate(event, apiKey.id)}>
                              <div className={styles.field}>
                                <span>可访问知识库</span>
                                <ScopePicker
                                  knowledgeBases={knowledgeBases}
                                  scopeDrafts={scopeEditDrafts}
                                  onToggle={(knowledgeBaseId) => {
                                    setScopeEditDrafts((current) => {
                                      const next = { ...current }
                                      if (next[knowledgeBaseId]) {
                                        delete next[knowledgeBaseId]
                                      } else {
                                        next[knowledgeBaseId] = 'WRITE'
                                      }
                                      return next
                                    })
                                  }}
                                  onModeChange={(knowledgeBaseId, accessMode) => {
                                    setScopeEditDrafts((current) => ({
                                      ...current,
                                      [knowledgeBaseId]: accessMode,
                                    }))
                                  }}
                                  emptyMessage="当前工作区还没有知识库，暂时无法调整权限。"
                                />
                              </div>
                              <div className={styles.inlineFormFooter}>
                                <div className={styles.subtleText}>保存后立即生效。</div>
                                <div className={styles.keyActions}>
                                  <button
                                    type="button"
                                    className={styles.secondaryButton}
                                    onClick={() => {
                                      setEditingScopeKeyId(null)
                                      setScopeEditDrafts({})
                                    }}
                                  >
                                    取消
                                  </button>
                                  <button
                                    type="submit"
                                    className={styles.primaryButton}
                                    disabled={scopeCount(scopeEditDrafts) === 0}
                                  >
                                    保存权限
                                  </button>
                                </div>
                              </div>
                            </form>
                          ) : null}
                        </section>
                      )
                    }) : (
                      <div className={styles.emptyState}>当前主体还没有可用密钥，可直接新增一把。</div>
                    )}
                  </div>
                </article>
              )
            })}
          </div>
        ) : (
          <div className={styles.emptyState}>
            <div>当前工作区还没有机器主体。</div>
            {!showCreateForm ? (
              <div className={styles.emptyStateActions}>
                <button type="button" className={styles.primaryButton} onClick={handleToggleCreateForm}>
                  创建首个主体
                </button>
              </div>
            ) : null}
          </div>
        )}
      </section>

      {showCreateForm ? (
        <section className={styles.createSection}>
          <div className={styles.formSectionHeader}>
            <div>
              <strong>新建机器主体</strong>
              <span>创建主体并直接拿到首把可用密钥。</span>
            </div>
          </div>

          <form className={styles.form} onSubmit={handleCreateServiceAccount}>
            <div className={styles.formGrid}>
              <label className={styles.field}>
                <span>主体名称</span>
                <input
                  value={accountForm.name}
                  onChange={(event) => setAccountForm((current) => ({ ...current, name: event.target.value }))}
                  placeholder="例如：ERP 同步写入"
                  required
                />
              </label>
              <label className={styles.field}>
                <span>首把密钥名称</span>
                <input
                  value={accountForm.keyName}
                  onChange={(event) => setAccountForm((current) => ({ ...current, keyName: event.target.value }))}
                  placeholder="例如：生产写入密钥"
                  required
                />
              </label>
            </div>

            <label className={styles.field}>
              <span>说明</span>
              <textarea
                rows={3}
                value={accountForm.description}
                onChange={(event) => setAccountForm((current) => ({ ...current, description: event.target.value }))}
                placeholder="描述这个主体对应的系统或链路"
              />
            </label>

            <div className={styles.formGrid}>
              <label className={styles.field}>
                <span>首个密钥有效期（天）</span>
                <input
                  type="number"
                  min="1"
                  max="3650"
                  value={accountForm.expiresInDays}
                  onChange={(event) => setAccountForm((current) => ({ ...current, expiresInDays: event.target.value }))}
                />
              </label>
              <div className={styles.field}>
                <span>开放知识库</span>
                <div className={styles.inlineMetric}>{scopeCount(accountForm.scopeDrafts)} 个知识库</div>
              </div>
            </div>

            <div className={styles.field}>
              <span>首个密钥可访问的知识库</span>
              <ScopePicker
                knowledgeBases={knowledgeBases}
                scopeDrafts={accountForm.scopeDrafts}
                onToggle={(knowledgeBaseId) => toggleScope(setAccountForm, knowledgeBaseId)}
                onModeChange={(knowledgeBaseId, accessMode) => changeScopeMode(setAccountForm, knowledgeBaseId, accessMode)}
                emptyMessage="当前工作区还没有知识库，暂时无法创建密钥。"
              />
            </div>

            <div className={styles.footer}>
              <div className={styles.subtleText}>至少选择 1 个知识库。</div>
              <div className={styles.keyActions}>
                {serviceAccounts.length > 0 ? (
                  <button type="button" className={styles.secondaryButton} onClick={handleToggleCreateForm}>
                    取消
                  </button>
                ) : null}
                <button
                  type="submit"
                  className={styles.primaryButton}
                  disabled={submitting || knowledgeBases.length === 0 || scopeCount(accountForm.scopeDrafts) === 0}
                >
                  {submitting ? '创建中...' : '创建主体'}
                </button>
              </div>
            </div>
          </form>
        </section>
      ) : null}
    </section>
  )
}

export default ServiceAccessPanel
