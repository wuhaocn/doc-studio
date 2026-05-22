import { useEffect, useRef } from 'react'
import { EditorContent, useEditor } from '@tiptap/react'
import StarterKit from '@tiptap/starter-kit'
import Placeholder from '@tiptap/extension-placeholder'
import Image from '@tiptap/extension-image'
import DiagramExtension from '../../extensions/DiagramExtension'
import { DOCUMENT_FORMATS, normalizeRichTextEditorContent, sanitizeDocumentHtml } from '../../utils/documentContent'
import { saveDraft } from '../../utils/editorDraft'
import styles from './DocumentRichEditor.module.css'

const AUTOSAVE_INTERVAL = 30000

const DocumentRichEditor = ({
  focusMode = false,
  initialContent = '',
  placeholder = '开始输入内容...',
  saving = false,
  documentId,
  onCancel,
  onSave,
  onDirtyChange,
}) => {
  const lastContentRef = useRef(initialContent)
  const dirtyRef = useRef(false)

  const editor = useEditor({
    extensions: [
      StarterKit,
      Image,
      Placeholder.configure({
        placeholder,
      }),
      DiagramExtension,
    ],
    content: normalizeRichTextEditorContent(initialContent),
    onUpdate: () => {
      if (!dirtyRef.current) {
        dirtyRef.current = true
        onDirtyChange?.(true)
      }
    },
  })

  useEffect(() => {
    if (!editor || initialContent === lastContentRef.current) {
      return
    }
    editor.commands.setContent(normalizeRichTextEditorContent(initialContent))
    lastContentRef.current = initialContent
  }, [editor, initialContent])

  useEffect(() => {
    if (!editor || !documentId) return
    const timer = setInterval(() => {
      if (dirtyRef.current) {
        saveDraft(documentId, sanitizeDocumentHtml(editor.getHTML()))
      }
    }, AUTOSAVE_INTERVAL)
    return () => clearInterval(timer)
  }, [editor, documentId])

  const handleInsertImage = () => {
    if (!editor) {
      return
    }
    const url = window.prompt('请输入图片 URL')
    if (url) {
      editor.chain().focus().setImage({ src: url }).run()
    }
  }

  const handleInsertMermaid = () => {
    if (!editor) {
      return
    }
    const code = window.prompt('请输入 Mermaid 图表代码')
    if (code?.trim()) {
      editor.chain().focus().insertDiagram({
        diagramType: 'mermaid',
        content: code.trim(),
        width: '100%',
        height: '360px',
      }).run()
    }
  }

  const handleSave = async () => {
    if (!editor) {
      return
    }
    await onSave({
      format: DOCUMENT_FORMATS.RICH_TEXT,
      content: sanitizeDocumentHtml(editor.getHTML()),
    })
    dirtyRef.current = false
    onDirtyChange?.(false)
  }

  const tools = [
    {
      key: 'paragraph',
      label: '正文',
      active: editor?.isActive('paragraph'),
      onClick: () => editor?.chain().focus().setParagraph().run(),
    },
    {
      key: 'h1',
      label: 'H1',
      active: editor?.isActive('heading', { level: 1 }),
      onClick: () => editor?.chain().focus().toggleHeading({ level: 1 }).run(),
    },
    {
      key: 'h2',
      label: 'H2',
      active: editor?.isActive('heading', { level: 2 }),
      onClick: () => editor?.chain().focus().toggleHeading({ level: 2 }).run(),
    },
    {
      key: 'bold',
      label: '加粗',
      active: editor?.isActive('bold'),
      onClick: () => editor?.chain().focus().toggleBold().run(),
    },
    {
      key: 'bullet',
      label: '列表',
      active: editor?.isActive('bulletList'),
      onClick: () => editor?.chain().focus().toggleBulletList().run(),
    },
    {
      key: 'quote',
      label: '引用',
      active: editor?.isActive('blockquote'),
      onClick: () => editor?.chain().focus().toggleBlockquote().run(),
    },
    {
      key: 'code',
      label: '代码块',
      active: editor?.isActive('codeBlock'),
      onClick: () => editor?.chain().focus().toggleCodeBlock().run(),
    },
  ]
  const structureTools = tools.slice(0, 3)
  const formatTools = tools.slice(3)
  const insertTools = [
    {
      key: 'image',
      label: '图片',
      onClick: handleInsertImage,
    },
    {
      key: 'mermaid',
      label: '图表',
      onClick: handleInsertMermaid,
    },
  ]

  return (
    <div className={`${styles.editorShell} ${focusMode ? styles.focusMode : ''}`}>
      <div className={styles.toolbar}>
        <div className={styles.toolbarInner}>
          <div className={styles.toolbarMain}>
            <div className={styles.toolSection}>
              <span className={styles.toolSectionLabel}>文本层级</span>
              <div className={styles.toolGroup}>
                {structureTools.map((tool) => (
                  <button
                    key={tool.key}
                    type="button"
                    className={`${styles.toolButton} ${tool.active ? styles.active : ''}`}
                    onClick={tool.onClick}
                  >
                    {tool.label}
                  </button>
                ))}
              </div>
            </div>
            <div className={styles.toolDivider} />
            <div className={styles.toolSection}>
              <span className={styles.toolSectionLabel}>常用格式</span>
              <div className={styles.toolGroup}>
                {formatTools.map((tool) => (
                  <button
                    key={tool.key}
                    type="button"
                    className={`${styles.toolButton} ${tool.active ? styles.active : ''}`}
                    onClick={tool.onClick}
                  >
                    {tool.label}
                  </button>
                ))}
              </div>
            </div>
          </div>
          <div className={styles.toolbarAside}>
            <span className={styles.toolSectionLabel}>插入内容</span>
            <div className={styles.toolGroup}>
              {insertTools.map((tool) => (
                <button key={tool.key} type="button" className={styles.toolButton} onClick={tool.onClick}>
                  {tool.label}
                </button>
              ))}
            </div>
          </div>
        </div>
      </div>

      <div className={styles.editorBody}>
        <div className={styles.editorCanvas}>
          <div className={styles.editorPaper}>{editor && <EditorContent editor={editor} />}</div>
        </div>
      </div>

      <div className={styles.footer}>
        <div className={styles.footerInner}>
          <div className={styles.footerHint}>保存后生成新版本，历史内容可随时回滚。</div>
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
    </div>
  )
}

export default DocumentRichEditor
