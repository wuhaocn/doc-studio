import { useEffect, useState } from 'react'
import styles from './DocumentActionModal.module.css'

const EMPTY_FORM = {
  title: '',
  summary: '',
  parentId: 0,
}

const DocumentActionModal = ({
  open,
  mode = 'create',
  docType = 'DOC',
  initialValues,
  folderOptions = [],
  submitting = false,
  errorMessage = '',
  onClose,
  onSubmit,
}) => {
  const [form, setForm] = useState(EMPTY_FORM)
  const [localError, setLocalError] = useState('')
  const [advancedOpen, setAdvancedOpen] = useState(false)

  useEffect(() => {
    if (!open) {
      return
    }

    setLocalError('')
    setForm({
      title: initialValues?.title || '',
      summary: initialValues?.summary || '',
      parentId: initialValues?.parentId ?? 0,
    })
    setAdvancedOpen(mode === 'edit')
  }, [initialValues, mode, open])

  if (!open) {
    return null
  }

  const typeLabel = docType === 'FOLDER' ? '目录' : '文档'
  const titleLabel = mode === 'create' ? `新建${typeLabel}` : `编辑${typeLabel}`
  const showAdvancedSettings = mode === 'edit' || advancedOpen
  const createHint = docType === 'FOLDER'
    ? '先输入目录名称，创建后再整理结构。'
    : '先输入文档标题，创建后会直接进入编辑页。'

  const handleChange = (key, value) => {
    if (localError) {
      setLocalError('')
    }

    setForm((current) => ({
      ...current,
      [key]: value,
    }))
  }

  const handleSubmit = async (event) => {
    event.preventDefault()

    if (!form.title.trim()) {
      setLocalError(`${typeLabel}名称不能为空`)
      return
    }

    await onSubmit({
      title: form.title.trim(),
      parentId: Number(form.parentId || 0),
      summary: form.summary.trim() || undefined,
    })
  }

  return (
    <div className={styles.overlay} role="dialog" aria-modal="true">
      <div className={styles.modal}>
        <div className={styles.header}>
          <div>
            <p className={styles.eyebrow}>{mode === 'create' ? 'Create Tree Node' : 'Update Tree Node'}</p>
            <h2 className={styles.title}>{titleLabel}</h2>
          </div>
          <button type="button" className={styles.closeButton} onClick={onClose}>
            关闭
          </button>
        </div>

        <form className={styles.form} onSubmit={handleSubmit}>
          <div className={styles.typeTag}>{typeLabel}</div>
          {mode === 'create' && <p className={styles.quickHint}>{createHint}</p>}

          <label className={styles.field}>
            <span>名称</span>
            <input
              value={form.title}
              onChange={(event) => handleChange('title', event.target.value)}
              placeholder={docType === 'FOLDER' ? '例如：交付附录' : '例如：开箱验收规范'}
              required
            />
          </label>

          {(mode === 'create' || docType === 'DOC') && (
            <button
              type="button"
              className={styles.advancedToggle}
              onClick={() => setAdvancedOpen((current) => !current)}
            >
              {showAdvancedSettings ? '收起更多设置' : '更多设置'}
            </button>
          )}

          {showAdvancedSettings && (
            <>
              <label className={styles.field}>
                <span>父级目录</span>
                <select
                  value={form.parentId}
                  onChange={(event) => handleChange('parentId', event.target.value)}
                >
                  {folderOptions.map((option) => (
                    <option key={option.id} value={option.id}>
                      {option.label}
                    </option>
                  ))}
                </select>
              </label>

              {docType === 'DOC' && (
                <label className={styles.field}>
                  <span>摘要</span>
                  <textarea
                    value={form.summary}
                    rows={4}
                    onChange={(event) => handleChange('summary', event.target.value)}
                    placeholder="可选，用于树节点预览和摘要展示"
                  />
                </label>
              )}
            </>
          )}

          {(localError || errorMessage) && (
            <div className={styles.errorMessage}>{localError || errorMessage}</div>
          )}

          <div className={styles.footer}>
            <button type="button" className={styles.secondaryButton} onClick={onClose}>
              取消
            </button>
            <button type="submit" className={styles.primaryButton} disabled={submitting}>
              {submitting ? '提交中...' : mode === 'create' ? `创建${typeLabel}` : '保存变更'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

export default DocumentActionModal
