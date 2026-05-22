import { useEffect, useMemo, useRef, useState } from 'react'
import DocumentRenderedContent from './DocumentRenderedContent'
import { getDocumentRenderState } from '../../utils/documentContent'
import { saveDraft } from '../../utils/editorDraft'
import styles from './DocumentSourceEditor.module.css'

const AUTOSAVE_INTERVAL = 30000

const PLACEHOLDER_BY_FORMAT = {
  MARKDOWN: '# 标题\n\n开始输入 Markdown 正文...',
  HTML: '<article>\n  <h1>标题</h1>\n  <p>开始输入 HTML 正文...</p>\n</article>',
}

const FORMAT_LABEL = {
  MARKDOWN: 'Markdown',
  HTML: 'HTML',
}

const DocumentSourceEditor = ({
  focusMode = false,
  format = 'MARKDOWN',
  initialContent = '',
  saving = false,
  documentId,
  onCancel,
  onSave,
  onDirtyChange,
}) => {
  const [content, setContent] = useState(initialContent || '')
  const lastContentRef = useRef(initialContent || '')
  const dirtyRef = useRef(false)

  useEffect(() => {
    const nextContent = initialContent || ''
    setContent(nextContent)
    lastContentRef.current = nextContent
    dirtyRef.current = false
    onDirtyChange?.(false)
  }, [initialContent, onDirtyChange])

  useEffect(() => {
    if (!documentId) {
      return undefined
    }
    const timer = setInterval(() => {
      if (dirtyRef.current) {
        saveDraft(documentId, content)
      }
    }, AUTOSAVE_INTERVAL)
    return () => clearInterval(timer)
  }, [content, documentId])

  const renderState = useMemo(() => {
    return getDocumentRenderState({
      format,
      content,
      contentText: '',
    })
  }, [content, format])

  const handleChange = (event) => {
    const nextValue = event.target.value
    setContent(nextValue)
    if (!dirtyRef.current && nextValue !== lastContentRef.current) {
      dirtyRef.current = true
      onDirtyChange?.(true)
    } else if (dirtyRef.current && nextValue === lastContentRef.current) {
      dirtyRef.current = false
      onDirtyChange?.(false)
    }
  }

  const handleSave = async () => {
    await onSave({
      format,
      content,
    })
    lastContentRef.current = content
    dirtyRef.current = false
    onDirtyChange?.(false)
  }

  return (
    <div className={`${styles.editorShell} ${focusMode ? styles.focusMode : ''}`}>
      <div className={styles.header}>
        <div>
          <div className={styles.eyebrow}>源码编辑</div>
          <h2 className={styles.title}>{FORMAT_LABEL[format] || format}</h2>
          <p className={styles.description}>左侧维护原始正文，右侧实时查看最终展示效果。</p>
        </div>
        <div className={styles.meta}>
          <span>{content.length} 字符</span>
          <span>{renderState.plainText.length} 纯文本字符</span>
        </div>
      </div>

      <div className={styles.workspace}>
        <section className={styles.panel}>
          <div className={styles.panelHeader}>
            <span>源码</span>
            <span>{FORMAT_LABEL[format] || format}</span>
          </div>
          <textarea
            className={styles.textarea}
            value={content}
            onChange={handleChange}
            spellCheck={format !== 'HTML'}
            placeholder={PLACEHOLDER_BY_FORMAT[format] || '开始输入正文...'}
          />
        </section>

        <section className={styles.panel}>
          <div className={styles.panelHeader}>
            <span>预览</span>
            <span>只读</span>
          </div>
          <div className={styles.preview}>
            {renderState.hasRenderedContent ? (
              <DocumentRenderedContent
                className={styles.previewContent}
                html={renderState.html}
                variant="compact"
              />
            ) : (
              <div className={styles.previewEmpty}>当前正文为空，保存后会展示为空白文档。</div>
            )}
          </div>
        </section>
      </div>

      <div className={styles.footer}>
        <div className={styles.footerHint}>保存后生成新版本，Markdown 与 HTML 会按当前格式直接渲染。</div>
        <div className={styles.footerActions}>
          <button type="button" className={styles.secondaryButton} onClick={onCancel}>
            取消
          </button>
          <button type="button" className={styles.primaryButton} disabled={saving} onClick={handleSave}>
            {saving ? '保存中...' : '保存并生成版本'}
          </button>
        </div>
      </div>
    </div>
  )
}

export default DocumentSourceEditor
