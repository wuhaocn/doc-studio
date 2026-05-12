import { useEffect, useState } from 'react'
import { useEscapeKey } from '../../hooks/useEscapeKey'
import styles from './KnowledgeBaseFormModal.module.css'

const DEFAULT_FORM = {
  name: '',
  slug: '',
  description: '',
}

const KnowledgeBaseFormModal = ({
  mode = 'create',
  open,
  initialValues,
  submitting = false,
  errorMessage = '',
  onClose,
  onSubmit,
}) => {
  const [form, setForm] = useState(DEFAULT_FORM)
  const [localError, setLocalError] = useState('')
  useEscapeKey(open, onClose)

  useEffect(() => {
    if (!open) {
      return
    }

    setLocalError('')
    setForm({
      name: initialValues?.name || '',
      slug: initialValues?.slug || '',
      description: initialValues?.description || '',
    })
  }, [initialValues, open])

  if (!open) {
    return null
  }

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

    if (!form.name.trim()) {
      setLocalError('知识库名称不能为空')
      return
    }

    if (form.name.trim().length > 50) {
      setLocalError('知识库名称不能超过 50 个字符')
      return
    }

    await onSubmit({
      name: form.name.trim(),
      slug: form.slug.trim() || undefined,
      description: form.description.trim(),
    })
  }

  return (
    <div className={styles.overlay} role="dialog" aria-modal="true">
      <div className={styles.modal}>
        <div className={styles.header}>
          <div>
            <p className={styles.eyebrow}>知识库工作区</p>
            <h2 className={styles.title}>{mode === 'create' ? '新建知识库' : '知识库设置'}</h2>
            <p className={styles.description}>
              {mode === 'create' ? '先填写名称，创建后直接进入知识库继续新建文档。' : '调整名称和说明，保持知识库边界清晰。'}
            </p>
          </div>
          <button type="button" className={styles.closeButton} onClick={onClose}>
            关闭
          </button>
        </div>

        <form className={styles.form} onSubmit={handleSubmit}>
          <label className={styles.field}>
            <span>名称</span>
            <input
              value={form.name}
              onChange={(event) => handleChange('name', event.target.value)}
              placeholder="例如：售后维保知识库"
              required
            />
          </label>

          <label className={styles.field}>
            <span>标识</span>
            <input
              value={form.slug}
              onChange={(event) => handleChange('slug', event.target.value)}
              placeholder="可选，不填则自动生成"
            />
          </label>

          <label className={styles.field}>
            <span>简介</span>
            <textarea
              value={form.description}
              onChange={(event) => handleChange('description', event.target.value)}
              rows={4}
              placeholder="描述知识库面向的业务场景、对象和维护方式"
            />
          </label>

          {(localError || errorMessage) && (
            <div className={styles.errorMessage}>{localError || errorMessage}</div>
          )}

          <div className={styles.footer}>
            <button type="button" className={styles.secondaryButton} onClick={onClose}>
              取消
            </button>
            <button type="submit" className={styles.primaryButton} disabled={submitting}>
              {submitting ? '提交中...' : mode === 'create' ? '新建知识库' : '保存设置'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

export default KnowledgeBaseFormModal
