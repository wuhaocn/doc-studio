import { useEffect, useState } from 'react'
import { useEscapeKey } from '../../hooks/useEscapeKey'
import styles from './DocumentActionModal.module.css'

const DocumentBatchMoveModal = ({
  open,
  selectedCount = 0,
  folderOptions = [],
  submitting = false,
  errorMessage = '',
  initialParentId = 0,
  onClose,
  onSubmit,
}) => {
  const [parentId, setParentId] = useState(initialParentId)
  useEscapeKey(open, onClose)

  useEffect(() => {
    if (!open) {
      return
    }
    setParentId(initialParentId)
  }, [initialParentId, open])

  if (!open) {
    return null
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    await onSubmit({
      parentId: Number(parentId || 0),
    })
  }

  return (
    <div className={styles.overlay} role="dialog" aria-modal="true">
      <div className={styles.modal}>
        <div className={styles.header}>
          <div>
            <p className={styles.eyebrow}>批量整理</p>
            <h2 className={styles.title}>批量移动节点</h2>
            <p className={styles.description}>把已选内容移动到新的目录中，继续整理文档结构。</p>
          </div>
          <button type="button" className={styles.closeButton} onClick={onClose}>
            关闭
          </button>
        </div>

        <form className={styles.form} onSubmit={handleSubmit}>
          <div className={styles.typeTag}>已选择 {selectedCount} 个节点</div>

          <label className={styles.field}>
            <span>目标目录</span>
            <select value={parentId} onChange={(event) => setParentId(event.target.value)}>
              {folderOptions.map((option) => (
                <option key={option.id} value={option.id}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>

          {errorMessage && <div className={styles.errorMessage}>{errorMessage}</div>}

          <div className={styles.footer}>
            <button type="button" className={styles.secondaryButton} onClick={onClose}>
              取消
            </button>
            <button type="submit" className={styles.primaryButton} disabled={submitting}>
              {submitting ? '移动中...' : '确认移动'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

export default DocumentBatchMoveModal
