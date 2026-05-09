import { useEffect, useMemo, useState } from 'react'
import { copyText } from '../../utils/copyText'
import styles from './DocumentReadLinkDrawer.module.css'

const DocumentReadLinkDrawer = ({
  open,
  documentId,
  title,
  onClose,
}) => {
  const [copied, setCopied] = useState(false)
  const documentLink = useMemo(() => {
    if (!documentId) {
      return ''
    }
    return `${window.location.origin}/docs/${documentId}`
  }, [documentId])

  useEffect(() => {
    if (open) {
      setCopied(false)
    }
  }, [open])

  if (!open) {
    return null
  }

  const handleCopyLink = async () => {
    try {
      await copyText(documentLink)
      setCopied(true)
      window.setTimeout(() => setCopied(false), 1800)
    } catch (error) {
      console.error('复制文档链接失败', error)
    }
  }

  return (
    <div className={styles.overlay} role="dialog" aria-modal="true">
      <div className={styles.drawer}>
        <div className={styles.header}>
          <div>
            <p className={styles.eyebrow}>复制阅读链接</p>
            <h2 className={styles.title}>{title}</h2>
            <p className={styles.description}>复制当前文档阅读页链接，实际访问权限仍按当前登录会话判定。</p>
          </div>
          <button type="button" className={styles.closeButton} onClick={onClose}>
            关闭
          </button>
        </div>

        <div className={styles.summaryBlock}>
          <div className={styles.summaryItem}>
            <span className={styles.summaryLabel}>访问方式</span>
            <strong>按当前登录态判定</strong>
          </div>
          <div className={styles.summaryItem}>
            <span className={styles.summaryLabel}>打开页面</span>
            <strong>文档阅读页</strong>
          </div>
        </div>

        <div className={styles.section}>
          <div className={styles.label}>复制阅读链接</div>
          <div className={styles.linkRow}>
            <input readOnly value={documentLink} className={styles.linkInput} />
            <button type="button" className={styles.primaryButton} onClick={handleCopyLink}>
              {copied ? '已复制' : '复制阅读链接'}
            </button>
          </div>
        </div>

        <div className={styles.note}>链接不会放宽权限；未登录或无权访问的用户仍会被拒绝。</div>
      </div>
    </div>
  )
}

export default DocumentReadLinkDrawer
