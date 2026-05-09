import { useEffect, useMemo, useState } from 'react'
import dayjs from 'dayjs'
import { serviceAccountApi } from '../../services/api/serviceAccountApi'
import { copyText } from '../../utils/copyText'
import styles from './ServiceAccountModal.module.css'

const DEFAULT_FORM = {
  name: '',
  description: '',
  keyName: '默认写入 key',
  expiresInDays: 90,
  accessMode: 'WRITE',
  knowledgeBaseIds: [],
}

const API_KEY_STATUS_LABELS = {
  0: '已禁用',
  1: '生效中',
  2: '已吊销',
}

const ServiceAccountModal = ({
  open,
  knowledgeBases = [],
  onClose,
  onChanged,
}) => {
  const [serviceAccounts, setServiceAccounts] = useState([])
  const [loading, setLoading] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [actionKey, setActionKey] = useState('')
  const [errorMessage, setErrorMessage] = useState('')
  const [feedback, setFeedback] = useState('')
  const [issuedKey, setIssuedKey] = useState(null)
  const [form, setForm] = useState(DEFAULT_FORM)

  const knowledgeBaseNameMap = useMemo(() => {
    return new Map(knowledgeBases.map((item) => [item.id, item.name]))
  }, [knowledgeBases])

  useEffect(() => {
    if (!open) {
      return
    }

    const nextKnowledgeBaseIds = knowledgeBases.length > 0
      ? [knowledgeBases[0].id]
      : []

    setForm({
      ...DEFAULT_FORM,
      knowledgeBaseIds: nextKnowledgeBaseIds,
    })
    setErrorMessage('')
    setFeedback('')
    setIssuedKey(null)
    void loadServiceAccounts()
  }, [knowledgeBases, open])

  if (!open) {
    return null
  }

  async function loadServiceAccounts() {
    try {
      setLoading(true)
      const response = await serviceAccountApi.listServiceAccounts()
      setServiceAccounts(response?.data || [])
    } catch (error) {
      console.error('加载 service account 失败', error)
      setServiceAccounts([])
      setErrorMessage(error?.message || '加载 API key 管理列表失败，请稍后重试')
    } finally {
      setLoading(false)
    }
  }

  const handleKnowledgeBaseToggle = (knowledgeBaseId) => {
    setForm((current) => {
      const exists = current.knowledgeBaseIds.includes(knowledgeBaseId)
      const knowledgeBaseIds = exists
        ? current.knowledgeBaseIds.filter((item) => item !== knowledgeBaseId)
        : [...current.knowledgeBaseIds, knowledgeBaseId]

      return {
        ...current,
        knowledgeBaseIds,
      }
    })
  }

  const handleCreateServiceAccount = async (event) => {
    event.preventDefault()

    try {
      setSubmitting(true)
      setErrorMessage('')
      setFeedback('')
      const response = await serviceAccountApi.createServiceAccount({
        ...form,
        name: form.name.trim(),
        description: form.description.trim() || undefined,
        keyName: form.keyName.trim(),
        expiresInDays: Number(form.expiresInDays) || 90,
      })
      setIssuedKey(response?.data || null)
      setFeedback('机器主体和首个 API key 已签发，明文 key 只展示这一次。')
      await loadServiceAccounts()
      await onChanged?.()
      setForm((current) => ({
        ...current,
        name: '',
        description: '',
      }))
    } catch (error) {
      console.error('创建 service account 失败', error)
      setErrorMessage(error?.message || '创建 service account 失败，请稍后重试')
    } finally {
      setSubmitting(false)
    }
  }

  const handleCopyIssuedKey = async () => {
    if (!issuedKey?.plainTextKey) {
      return
    }

    try {
      await copyText(issuedKey.plainTextKey)
      setFeedback('API key 已复制')
    } catch (error) {
      console.error('复制 API key 失败', error)
      setErrorMessage('复制 API key 失败，请手动保存')
    }
  }

  const runApiKeyAction = async (loadingId, action, successMessage) => {
    try {
      setActionKey(loadingId)
      setErrorMessage('')
      setFeedback('')
      const response = await action()
      if (response?.data?.plainTextKey) {
        setIssuedKey(response.data)
      }
      await loadServiceAccounts()
      await onChanged?.()
      setFeedback(successMessage)
    } catch (error) {
      console.error('执行 API key 操作失败', error)
      setErrorMessage(error?.message || '执行 API key 操作失败，请稍后重试')
    } finally {
      setActionKey('')
    }
  }

  return (
    <div className={styles.overlay} role="dialog" aria-modal="true">
      <div className={styles.modal}>
        <div className={styles.header}>
          <div>
            <p className={styles.eyebrow}>开放写入</p>
            <h2 className={styles.title}>Service Account 与 API key</h2>
            <p className={styles.description}>把知识库文档操作收口为机器主体、作用域和可轮换 key，便于外部系统按边界写入文档。</p>
          </div>
          <button type="button" className={styles.closeButton} onClick={onClose}>
            关闭
          </button>
        </div>

        <form className={styles.form} onSubmit={handleCreateServiceAccount}>
          <div className={styles.formGrid}>
            <label className={styles.field}>
              <span>机器主体名称</span>
              <input
                value={form.name}
                onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))}
                placeholder="例如：ERP 同步写入"
                required
              />
            </label>
            <label className={styles.field}>
              <span>首个 API key 名称</span>
              <input
                value={form.keyName}
                onChange={(event) => setForm((current) => ({ ...current, keyName: event.target.value }))}
                placeholder="例如：生产写入 key"
                required
              />
            </label>
          </div>

          <label className={styles.field}>
            <span>说明</span>
            <textarea
              rows={3}
              value={form.description}
              onChange={(event) => setForm((current) => ({ ...current, description: event.target.value }))}
              placeholder="描述这个 key 会被哪个系统、在什么链路里调用"
            />
          </label>

          <div className={styles.formGrid}>
            <label className={styles.field}>
              <span>访问模式</span>
              <select
                value={form.accessMode}
                onChange={(event) => setForm((current) => ({ ...current, accessMode: event.target.value }))}
              >
                <option value="WRITE">WRITE</option>
                <option value="READ">READ</option>
              </select>
            </label>
            <label className={styles.field}>
              <span>有效期（天）</span>
              <input
                type="number"
                min="1"
                max="3650"
                value={form.expiresInDays}
                onChange={(event) => setForm((current) => ({ ...current, expiresInDays: event.target.value }))}
              />
            </label>
          </div>

          <div className={styles.field}>
            <span>知识库作用域</span>
            <div className={styles.scopeGrid}>
              {knowledgeBases.length > 0 ? (
                knowledgeBases.map((knowledgeBase) => {
                  const checked = form.knowledgeBaseIds.includes(knowledgeBase.id)
                  return (
                    <label key={knowledgeBase.id} className={`${styles.scopeItem} ${checked ? styles.scopeItemChecked : ''}`}>
                      <input
                        type="checkbox"
                        checked={checked}
                        onChange={() => handleKnowledgeBaseToggle(knowledgeBase.id)}
                      />
                      <div>
                        <strong>{knowledgeBase.name}</strong>
                        <span>{knowledgeBase.documentCount || 0} 篇文档</span>
                      </div>
                    </label>
                  )
                })
              ) : (
                <div className={styles.emptyState}>当前工作区还没有知识库，暂时无法签发 API key。</div>
              )}
            </div>
          </div>

          {(errorMessage || feedback) ? (
            <div className={`${styles.message} ${errorMessage ? styles.messageError : styles.messageSuccess}`}>
              {errorMessage || feedback}
            </div>
          ) : null}

          {issuedKey?.plainTextKey ? (
            <div className={styles.issuedKeyCard}>
              <div className={styles.issuedKeyHeader}>
                <strong>明文 API key</strong>
                <span>只展示一次</span>
              </div>
              <textarea value={issuedKey.plainTextKey} readOnly rows={3} />
              <div className={styles.issuedKeyMeta}>
                <span>{issuedKey.apiKey?.name}</span>
                <span>{issuedKey.apiKey?.keyPrefix}</span>
              </div>
              <button type="button" className={styles.secondaryButton} onClick={handleCopyIssuedKey}>
                复制 API key
              </button>
            </div>
          ) : null}

          <div className={styles.footer}>
            <div className={styles.note}>首版作用域收口到知识库级 READ / WRITE，并支持轮换、禁用和吊销。</div>
            <button type="submit" className={styles.primaryButton} disabled={submitting || knowledgeBases.length === 0}>
              {submitting ? '签发中...' : '创建机器主体并签发 key'}
            </button>
          </div>
        </form>

        <section className={styles.listSection}>
          <div className={styles.listHeader}>
            <strong>已签发主体</strong>
            <span>{serviceAccounts.length} 个</span>
          </div>

          {loading ? (
            <div className={styles.emptyState}>正在加载 API key 列表...</div>
          ) : serviceAccounts.length > 0 ? (
            <div className={styles.accountList}>
              {serviceAccounts.map((account) => (
                <article key={account.id} className={styles.accountCard}>
                  <div className={styles.accountHeader}>
                    <div>
                      <strong>{account.name}</strong>
                      <div className={styles.accountMeta}>
                        <span>创建于 {account.createdAt ? dayjs(account.createdAt).format('YYYY-MM-DD HH:mm') : '刚刚'}</span>
                        {account.description ? <span>{account.description}</span> : null}
                      </div>
                    </div>
                    <span className={styles.accountBadge}>{account.apiKeys?.length || 0} 个 key</span>
                  </div>

                  <div className={styles.keyList}>
                    {(account.apiKeys || []).map((apiKey) => {
                      const loadingIdPrefix = `${account.id}:${apiKey.id}`
                      const accessSummary = apiKey.knowledgeBaseIds
                        ?.map((knowledgeBaseId) => knowledgeBaseNameMap.get(knowledgeBaseId) || `知识库 #${knowledgeBaseId}`)
                        .join('、')
                      const statusLabel = apiKey.expired
                        ? '已过期'
                        : API_KEY_STATUS_LABELS[apiKey.status] || '未知状态'

                      return (
                        <section key={apiKey.id} className={styles.keyCard}>
                          <div className={styles.keyTopline}>
                            <div>
                              <strong>{apiKey.name}</strong>
                              <div className={styles.keyMeta}>
                                <span>{apiKey.keyPrefix}</span>
                                <span>{statusLabel}</span>
                                <span>{(apiKey.accessModes || []).join(' / ') || '无模式'}</span>
                              </div>
                            </div>
                            <div className={styles.keyActions}>
                              {apiKey.status === 1 && !apiKey.expired ? (
                                <button
                                  type="button"
                                  className={styles.secondaryButton}
                                  disabled={actionKey === `${loadingIdPrefix}:rotate`}
                                  onClick={() => runApiKeyAction(
                                    `${loadingIdPrefix}:rotate`,
                                    () => serviceAccountApi.rotateApiKey(apiKey.id, {}),
                                    'API key 已轮换，旧 key 已立即失效',
                                  )}
                                >
                                  {actionKey === `${loadingIdPrefix}:rotate` ? '轮换中...' : '轮换'}
                                </button>
                              ) : null}
                              {apiKey.status === 1 ? (
                                <button
                                  type="button"
                                  className={styles.secondaryButton}
                                  disabled={actionKey === `${loadingIdPrefix}:disable`}
                                  onClick={() => runApiKeyAction(
                                    `${loadingIdPrefix}:disable`,
                                    () => serviceAccountApi.disableApiKey(apiKey.id),
                                    'API key 已禁用',
                                  )}
                                >
                                  {actionKey === `${loadingIdPrefix}:disable` ? '禁用中...' : '禁用'}
                                </button>
                              ) : null}
                              {apiKey.status !== 2 ? (
                                <button
                                  type="button"
                                  className={styles.dangerButton}
                                  disabled={actionKey === `${loadingIdPrefix}:revoke`}
                                  onClick={() => runApiKeyAction(
                                    `${loadingIdPrefix}:revoke`,
                                    () => serviceAccountApi.revokeApiKey(apiKey.id),
                                    'API key 已吊销',
                                  )}
                                >
                                  {actionKey === `${loadingIdPrefix}:revoke` ? '吊销中...' : '吊销'}
                                </button>
                              ) : null}
                            </div>
                          </div>
                          <div className={styles.scopeMeta}>
                            <span>作用域</span>
                            <strong>{accessSummary || '未配置作用域'}</strong>
                          </div>
                          <div className={styles.scopeMeta}>
                            <span>到期</span>
                            <strong>{apiKey.expiresAt ? dayjs(apiKey.expiresAt).format('YYYY-MM-DD HH:mm') : '未设置'}</strong>
                          </div>
                          <div className={styles.scopeMeta}>
                            <span>最近使用</span>
                            <strong>{apiKey.lastUsedAt ? dayjs(apiKey.lastUsedAt).format('MM-DD HH:mm') : '尚未调用'}</strong>
                          </div>
                        </section>
                      )
                    })}
                  </div>
                </article>
              ))}
            </div>
          ) : (
            <div className={styles.emptyState}>当前工作区还没有 service account。</div>
          )}
        </section>
      </div>
    </div>
  )
}

export default ServiceAccountModal
