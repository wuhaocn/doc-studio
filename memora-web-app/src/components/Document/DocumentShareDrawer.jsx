import { useCallback, useEffect, useMemo, useState } from 'react'
import dayjs from 'dayjs'
import { documentShareApi } from '../../services/api/documentShareApi'
import { copyText } from '../../utils/copyText'
import styles from './DocumentShareDrawer.module.css'

const DEFAULT_FORM = {
  expiresInDays: 7,
  accessCode: '',
}

const SHARE_STATUS_LABELS = {
  0: '已撤销',
  1: '生效中',
}

const resolveShareUrl = (share) => {
  if (!share?.shareUrl) {
    return ''
  }

  if (share.shareUrl.startsWith('http://') || share.shareUrl.startsWith('https://')) {
    return share.shareUrl
  }
  return `${window.location.origin}${share.shareUrl}`
}

const DocumentShareDrawer = ({
  open,
  documentId,
  title,
  onClose,
  onChanged,
}) => {
  const [shares, setShares] = useState([])
  const [loading, setLoading] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [revokingShareId, setRevokingShareId] = useState(null)
  const [errorMessage, setErrorMessage] = useState('')
  const [feedback, setFeedback] = useState('')
  const [form, setForm] = useState(DEFAULT_FORM)

  const hasActiveShares = useMemo(() => {
    return shares.some((share) => share.status === 1 && !share.expired)
  }, [shares])

  const loadShares = useCallback(async () => {
    if (!documentId) {
      setShares([])
      return
    }

    try {
      setLoading(true)
      const response = await documentShareApi.listShares(documentId)
      setShares(response?.data || [])
    } catch (error) {
      console.error('加载受控分享失败', error)
      setShares([])
      setErrorMessage(error?.message || '加载受控分享失败，请稍后重试')
    } finally {
      setLoading(false)
    }
  }, [documentId])

  useEffect(() => {
    if (!open) {
      return
    }

    setForm(DEFAULT_FORM)
    setErrorMessage('')
    setFeedback('')
    void loadShares()
  }, [loadShares, open])

  if (!open) {
    return null
  }

  const handleCreateShare = async (event) => {
    event.preventDefault()

    try {
      setSubmitting(true)
      setErrorMessage('')
      setFeedback('')
      const response = await documentShareApi.createShare({
        documentId,
        expiresInDays: Number(form.expiresInDays) || 7,
        accessCode: form.accessCode.trim() || undefined,
      })
      const createdShare = response?.data
      await loadShares()
      await onChanged?.()
      setForm(DEFAULT_FORM)
      setFeedback(createdShare?.accessCodeProtected
        ? '新的受控分享已创建，访问方需要访问码。'
        : '新的受控分享已创建，可直接复制外链。')
    } catch (error) {
      console.error('创建受控分享失败', error)
      setErrorMessage(error?.message || '创建受控分享失败，请稍后重试')
    } finally {
      setSubmitting(false)
    }
  }

  const handleCopyShare = async (share) => {
    try {
      await copyText(resolveShareUrl(share))
      setFeedback('受控分享链接已复制')
    } catch (error) {
      console.error('复制受控分享链接失败', error)
      setErrorMessage('复制受控分享链接失败，请手动复制')
    }
  }

  const handleRevokeShare = async (shareId) => {
    if (!window.confirm('撤销后，外部链接将立即失效。确认继续吗？')) {
      return
    }

    try {
      setRevokingShareId(shareId)
      setErrorMessage('')
      await documentShareApi.revokeShare(shareId)
      await loadShares()
      await onChanged?.()
      setFeedback('受控分享已撤销，外部访问将立即被阻断')
    } catch (error) {
      console.error('撤销受控分享失败', error)
      setErrorMessage(error?.message || '撤销受控分享失败，请稍后重试')
    } finally {
      setRevokingShareId(null)
    }
  }

  return (
    <div className={styles.overlay} role="dialog" aria-modal="true">
      <div className={styles.drawer}>
        <div className={styles.header}>
          <div>
            <p className={styles.eyebrow}>受控分享</p>
            <h2 className={styles.title}>{title}</h2>
            <p className={styles.description}>为当前文档生成外部只读链接，并按过期时间与访问码进行边界控制。</p>
          </div>
          <button type="button" className={styles.closeButton} onClick={onClose}>
            关闭
          </button>
        </div>

        <div className={styles.summaryBlock}>
          <div className={styles.summaryItem}>
            <span>分享范围</span>
            <strong>仅当前文档</strong>
          </div>
          <div className={styles.summaryItem}>
            <span>访问权限</span>
            <strong>外部只读</strong>
          </div>
          <div className={styles.summaryItem}>
            <span>当前状态</span>
            <strong>{hasActiveShares ? '已有生效分享' : '尚未创建分享'}</strong>
          </div>
        </div>

        <form className={styles.form} onSubmit={handleCreateShare}>
          <div className={styles.formGrid}>
            <label className={styles.field}>
              <span>有效期（天）</span>
              <input
                type="number"
                min="1"
                max="365"
                value={form.expiresInDays}
                onChange={(event) => setForm((current) => ({ ...current, expiresInDays: event.target.value }))}
              />
            </label>
            <label className={styles.field}>
              <span>访问码</span>
              <input
                value={form.accessCode}
                onChange={(event) => setForm((current) => ({ ...current, accessCode: event.target.value }))}
                placeholder="可选，留空则直接访问"
                maxLength={32}
              />
            </label>
          </div>

          {(errorMessage || feedback) ? (
            <div className={`${styles.message} ${errorMessage ? styles.messageError : styles.messageSuccess}`}>
              {errorMessage || feedback}
            </div>
          ) : null}

          <div className={styles.footer}>
            <div className={styles.note}>受控分享访问会进入审计；访问码不会再次明文展示，请创建后立即保存。</div>
            <button type="submit" className={styles.primaryButton} disabled={submitting}>
              {submitting ? '创建中...' : '创建受控分享'}
            </button>
          </div>
        </form>

        <section className={styles.historySection}>
          <div className={styles.historyHeader}>
            <strong>已有分享</strong>
            <span>{shares.length} 条</span>
          </div>

          {loading ? (
            <div className={styles.emptyState}>正在加载分享列表...</div>
          ) : shares.length > 0 ? (
            <div className={styles.shareList}>
              {shares.map((share) => {
                const shareUrl = resolveShareUrl(share)
                const statusText = share.expired && share.status === 1
                  ? '已过期'
                  : SHARE_STATUS_LABELS[share.status] || '未知状态'

                return (
                  <article key={share.id} className={styles.shareItem}>
                    <div className={styles.shareTopline}>
                      <strong>{statusText}</strong>
                      <span>{share.accessCodeProtected ? '需访问码' : '直接访问'}</span>
                    </div>
                    <div className={styles.shareLinkRow}>
                      <input readOnly value={shareUrl} className={styles.shareInput} />
                      <button type="button" className={styles.secondaryButton} onClick={() => handleCopyShare(share)}>
                        复制
                      </button>
                    </div>
                    <div className={styles.shareMeta}>
                      <span>到期 {share.expiresAt ? dayjs(share.expiresAt).format('YYYY-MM-DD HH:mm') : '未设置'}</span>
                      <span>创建于 {share.createdAt ? dayjs(share.createdAt).format('MM-DD HH:mm') : '刚刚'}</span>
                      <span>{share.lastAccessedAt ? `最近访问 ${dayjs(share.lastAccessedAt).format('MM-DD HH:mm')}` : '尚未访问'}</span>
                    </div>
                    {share.status === 1 && !share.expired ? (
                      <div className={styles.shareActions}>
                        <button
                          type="button"
                          className={styles.secondaryButton}
                          disabled={revokingShareId === share.id}
                          onClick={() => handleRevokeShare(share.id)}
                        >
                          {revokingShareId === share.id ? '撤销中...' : '撤销分享'}
                        </button>
                      </div>
                    ) : null}
                  </article>
                )
              })}
            </div>
          ) : (
            <div className={styles.emptyState}>当前文档还没有受控分享。</div>
          )}
        </section>
      </div>
    </div>
  )
}

export default DocumentShareDrawer
