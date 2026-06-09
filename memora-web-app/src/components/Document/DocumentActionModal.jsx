import { useEffect, useState } from 'react'
import { useEscapeKey } from '../../hooks/useEscapeKey'
import { DOCUMENT_QUICK_EXAMPLES, buildDocumentQuickExample } from '../../utils/documentExamples'
import styles from './DocumentActionModal.module.css'

const EMPTY_FORM = {
  title: '',
  summary: '',
  parentId: 0,
  format: 'RICH_TEXT',
  content: '',
  exampleId: '',
}

const FORMAT_OPTIONS = [
  { value: 'RICH_TEXT', label: '富文本编辑器' },
  { value: 'MARKDOWN', label: 'Markdown 源码' },
  { value: 'HTML', label: 'H5 源码' },
]

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
  useEscapeKey(open, onClose)

  useEffect(() => {
    if (!open) {
      return
    }

    setLocalError('')
    setForm({
      title: initialValues?.title || '',
      summary: initialValues?.summary || '',
      parentId: initialValues?.parentId ?? 0,
      format: initialValues?.format || 'RICH_TEXT',
      content: initialValues?.content || '',
      exampleId: '',
    })
    setAdvancedOpen(mode === 'edit')
  }, [initialValues, mode, open])

  if (!open) {
    return null
  }

  const typeLabel = docType === 'FOLDER' ? '目录' : '文档'
  const titleLabel = mode === 'create' ? `新建${typeLabel}` : `整理${typeLabel}`
  const showAdvancedSettings = mode === 'edit' || advancedOpen
  const createHint = docType === 'FOLDER'
    ? '先输入目录名称，创建后再整理结构。'
    : '先输入文档标题并选择正文格式，创建后会直接进入对应编辑器。'

  const handleChange = (key, value) => {
    if (localError) {
      setLocalError('')
    }

    setForm((current) => ({
      ...current,
      [key]: value,
    }))
  }

  const handleFormatChange = (value) => {
    if (localError) {
      setLocalError('')
    }

    setForm((current) => ({
      ...current,
      format: value,
      content: '',
      exampleId: '',
    }))
  }

  const handleApplyExample = (example) => {
    if (localError) {
      setLocalError('')
    }

    setAdvancedOpen(true)
    setForm((current) => {
      const exampleTitle = current.title.trim() || example.title
      const starter = buildDocumentQuickExample(example.format, exampleTitle)
      return {
        ...current,
        title: current.title.trim() || starter.title,
        summary: current.summary.trim() || starter.summary,
        format: starter.format,
        content: starter.content,
        exampleId: starter.id,
      }
    })
  }

  const handleSubmit = async (event) => {
    event.preventDefault()

    if (!form.title.trim()) {
      setLocalError(`${typeLabel}名称不能为空`)
      return
    }

    const exampleContent = form.exampleId
      ? buildDocumentQuickExample(form.format, form.title).content
      : form.content

    await onSubmit({
      title: form.title.trim(),
      parentId: Number(form.parentId || 0),
      summary: form.summary.trim() || undefined,
      format: docType === 'DOC' ? form.format : undefined,
      content: docType === 'DOC' && exampleContent ? exampleContent : undefined,
    })
  }

  return (
    <div className={styles.overlay} role="dialog" aria-modal="true">
      <div className={styles.modal}>
        <div className={styles.header}>
          <div>
            <p className={styles.eyebrow}>{docType === 'FOLDER' ? '目录结构' : '文档内容'}</p>
            <h2 className={styles.title}>{titleLabel}</h2>
            <p className={styles.description}>
              {mode === 'create' ? createHint : '调整标题、目录位置和摘要信息。'}
            </p>
          </div>
          <button type="button" className={styles.closeButton} onClick={onClose}>
            关闭
          </button>
        </div>

        <form className={styles.form} onSubmit={handleSubmit}>
          <div className={styles.typeTag}>{typeLabel}</div>

          <label className={styles.field}>
            <span>{docType === 'FOLDER' ? '名称' : '标题'}</span>
            <input
              value={form.title}
              onChange={(event) => handleChange('title', event.target.value)}
              placeholder={docType === 'FOLDER' ? '例如：交付附录' : '例如：开箱验收规范'}
              required
            />
          </label>

          {docType === 'DOC' ? (
            <label className={styles.field}>
              <span>正文格式</span>
              <select
                value={form.format}
                onChange={(event) => handleFormatChange(event.target.value)}
                disabled={mode !== 'create'}
              >
                {FORMAT_OPTIONS.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>
          ) : null}

          {mode === 'create' && docType === 'DOC' ? (
            <div className={styles.quickExamples}>
              <div className={styles.quickExampleHeader}>
                <div>
                  <strong>快速示例</strong>
                  <span>选择一种产物示例，会填充标题、摘要和首版正文。</span>
                </div>
              </div>
              <div className={styles.quickExampleList}>
                {DOCUMENT_QUICK_EXAMPLES.map((example) => {
                  const active = form.exampleId === example.id
                  return (
                    <button
                      key={example.id}
                      type="button"
                      className={`${styles.quickExampleButton} ${active ? styles.quickExampleButtonActive : ''}`}
                      onClick={() => handleApplyExample(example)}
                    >
                      <span>{example.label}</span>
                      <strong>{example.title}</strong>
                      <em>{example.difference}</em>
                      <small>{active ? '已套用，可直接创建' : `生成 ${example.outputLabel} 首版正文`}</small>
                    </button>
                  )
                })}
              </div>
              <p className={styles.quickHint}>
                H5 示例会创建 HTML 源码草稿，富文本用于可视化编辑，Markdown 用于结构化源码。
              </p>
            </div>
          ) : null}

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
              {submitting ? '提交中...' : mode === 'create' ? `新建${typeLabel}` : '保存调整'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

export default DocumentActionModal
