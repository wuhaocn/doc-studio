import { useEffect, useMemo, useState } from 'react'
import styles from './DocumentShareDrawer.module.css'

const copyText = async (text) => {
  if (navigator.clipboard?.writeText) {
    await navigator.clipboard.writeText(text)
    return
  }

  const textarea = document.createElement('textarea')
  textarea.value = text
  textarea.setAttribute('readonly', 'readonly')
  textarea.style.position = 'absolute'
  textarea.style.left = '-9999px'
  document.body.appendChild(textarea)
  textarea.select()
  document.execCommand('copy')
  document.body.removeChild(textarea)
}

const DocumentShareDrawer = ({
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
    return `${window.location.origin}/docs/${documentId}?share=1`
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
            <p className={styles.eyebrow}>分享文档</p>
            <h2 className={styles.title}>复制只读链接</h2>
            <p className={styles.description}>{title}</p>
          </div>
          <button type="button" className={styles.closeButton} onClick={onClose}>
            关闭
          </button>
        </div>

        <div className={styles.summaryRow}>
          <div className={styles.summaryCard}>
            <div className={styles.label}>访问方式</div>
            <strong>租户内只读</strong>
            <p>对方打开后直接进入阅读页，不展示编辑入口。</p>
          </div>
          <div className={styles.summaryCard}>
            <div className={styles.label}>目标页面</div>
            <strong>文档阅读页</strong>
            <p>适合发给评审、协作成员或只需查看内容的人。</p>
          </div>
        </div>

        <div className={styles.section}>
          <div className={styles.label}>复制分享链接</div>
          <div className={styles.linkRow}>
            <input readOnly value={documentLink} className={styles.linkInput} />
            <button type="button" className={styles.primaryButton} onClick={handleCopyLink}>
              {copied ? '已复制' : '复制分享链接'}
            </button>
          </div>
        </div>

        <div className={styles.note}>
          仅已登录且具备当前知识库访问权限的成员可以访问。公开匿名分享暂不开放。
        </div>
      </div>
    </div>
  )
}

export default DocumentShareDrawer
